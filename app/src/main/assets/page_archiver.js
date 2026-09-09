/*
 * Turns the rendered page into one self-contained HTML string.
 *
 * Written for Tuck rather than taken from SingleFile, which is AGPL-3.0 and would have
 * relicensed the whole app. This is the same idea at a fraction of the fidelity: pull the
 * things a page needs to render — stylesheets, images, fonts referenced from CSS — into the
 * document as text or data URIs, and drop everything that would make it behave like a live
 * page rather than a record of one.
 *
 * Returns a JSON string so the caller learns what it got and what it could not reach, instead
 * of being handed a page with silent holes in it.
 */
window.__tuckArchiveRun = function () {
  var MAX_ASSET_BYTES = 2 * 1024 * 1024;   // one image should not dominate the snapshot
  var MAX_TOTAL_BYTES = 12 * 1024 * 1024;  // a saved page, not a mirror of the site
  var TIMEOUT_MS = 8000;

  var budget = MAX_TOTAL_BYTES;
  var stats = { images: 0, styles: 0, failed: 0, skipped: 0 };

  function abs(url) {
    try { return new URL(url, document.baseURI).href; } catch (e) { return null; }
  }

  function fetchWithTimeout(url) {
    var controller = new AbortController();
    var timer = setTimeout(function () { controller.abort(); }, TIMEOUT_MS);
    return fetch(url, { signal: controller.signal, credentials: 'include' })
      .finally(function () { clearTimeout(timer); });
  }

  function toDataUri(url) {
    var target = abs(url);
    if (!target || target.indexOf('data:') === 0) return Promise.resolve(null);
    return fetchWithTimeout(target)
      .then(function (r) { return r.ok ? r.blob() : null; })
      .then(function (blob) {
        if (!blob) { stats.failed++; return null; }
        // Budget is checked before encoding: base64 inflates by a third, and a page of large
        // images should degrade to keeping their URLs rather than to a snapshot nobody can open.
        if (blob.size > MAX_ASSET_BYTES || blob.size > budget) { stats.skipped++; return null; }
        budget -= blob.size;
        return new Promise(function (resolve) {
          var reader = new FileReader();
          reader.onloadend = function () { resolve(reader.result); };
          reader.onerror = function () { stats.failed++; resolve(null); };
          reader.readAsDataURL(blob);
        });
      })
      .catch(function () { stats.failed++; return null; });
  }

  /* Stylesheets become <style> so the archive keeps its layout with no further requests. */
  function inlineStyleSheets() {
    var links = Array.prototype.slice.call(
      document.querySelectorAll('link[rel~="stylesheet"][href]')
    );
    return Promise.all(links.map(function (link) {
      var href = abs(link.getAttribute('href'));
      if (!href) return Promise.resolve();
      return fetchWithTimeout(href)
        .then(function (r) { return r.ok ? r.text() : null; })
        .then(function (css) {
          if (css === null) { stats.failed++; return; }
          // url(...) inside CSS is relative to the stylesheet, not the document, so it has to
          // be rewritten before the rule moves into the page.
          var rewritten = css.replace(/url\(\s*['"]?([^'")]+)['"]?\s*\)/g, function (match, ref) {
            if (ref.indexOf('data:') === 0) return match;
            try { return 'url("' + new URL(ref, href).href + '")'; } catch (e) { return match; }
          });
          var style = document.createElement('style');
          style.textContent = rewritten;
          link.parentNode.replaceChild(style, link);
          stats.styles++;
        })
        .catch(function () { stats.failed++; });
    }));
  }

  /* Images become data URIs, using what the browser actually chose from any srcset. */
  function inlineImages() {
    var images = Array.prototype.slice.call(document.querySelectorAll('img'));
    return Promise.all(images.map(function (img) {
      var chosen = img.currentSrc || img.src;
      if (!chosen) return Promise.resolve();
      return toDataUri(chosen).then(function (dataUri) {
        if (dataUri) {
          img.setAttribute('src', dataUri);
          stats.images++;
        } else if (chosen) {
          // Absolute so the original still resolves if the reader is online.
          img.setAttribute('src', chosen);
        }
        // srcset would send the renderer back to the network and override src.
        img.removeAttribute('srcset');
        img.removeAttribute('loading');
      });
    }));
  }

  /* A snapshot is a record, not a running page. */
  function stripBehaviour() {
    ['script', 'noscript', 'iframe', 'object', 'embed'].forEach(function (tag) {
      Array.prototype.slice.call(document.getElementsByTagName(tag)).forEach(function (node) {
        node.parentNode && node.parentNode.removeChild(node);
      });
    });
    Array.prototype.slice.call(document.querySelectorAll('link[rel~="preload"], link[rel~="prefetch"]'))
      .forEach(function (node) { node.parentNode && node.parentNode.removeChild(node); });
  }

  /* Relative links would resolve against the local file and go nowhere. */
  function absoluteLinks() {
    Array.prototype.slice.call(document.querySelectorAll('a[href]')).forEach(function (a) {
      var href = abs(a.getAttribute('href'));
      if (href) a.setAttribute('href', href);
    });
  }

  return inlineStyleSheets()
    .then(inlineImages)
    .then(function () {
      stripBehaviour();
      absoluteLinks();

      var base = document.createElement('base');
      base.setAttribute('href', document.baseURI);
      if (document.head) document.head.insertBefore(base, document.head.firstChild);

      var html = '<!DOCTYPE html>\n' + document.documentElement.outerHTML;
      return JSON.stringify({
        html: html,
        url: document.baseURI,
        title: document.title || '',
        images: stats.images,
        styles: stats.styles,
        failed: stats.failed,
        skipped: stats.skipped
      });
    })
    .catch(function (e) {
      return JSON.stringify({ error: String(e) });
    });
};
