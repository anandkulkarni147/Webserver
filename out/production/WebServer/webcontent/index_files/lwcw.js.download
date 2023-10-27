/*global jQuery:true */

(function() {
	function isValidJQuery() {
		var result = false;
		// load jquery and plugin if jquery not loaded, otherwise just load plugin
		// check for window.jQuery.fn.ajax function excludes jquery slim
		if ((typeof livewhale === 'object' && livewhale.jQuery)
				|| (window.jQuery && window.jQuery.fn && window.jQuery.fn.ajax)) {

			var version_parts = parseFloat(window.jQuery.fn.jquery).toString().split('.');
			var major_version = parseInt(version_parts[0], 10);
			var minor_version = parseInt(version_parts[1], 10);
			if (major_version > 1 || (major_version === 1 && minor_version >= 5)) {
				result = true;
			}
		}
		return result;
	}

	function getAggregateUrl(host, liveurl_dir, type, resources) {
		var urls = [];
		// build aggregate resource url
		for (var i = 0; i < resources.length; i++) {
			urls.push( resources[i].replace(/\//g, '%5C') );
		}
		return host + liveurl_dir + '/resource/' + type + '/' + urls.join('/');
	}

	function parseOptions(option_str) {
		var result = {},
				pairs;
		if (option_str && option_str.indexOf('&')) {
			pairs = option_str.split('&');
			for (var i = 0; i < pairs.length; i++) {
				var pair = pairs[i].split("=");
				result[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
			}
		}
		return result;
	}

	// dynamically load javascript and execute callback when finished
	function loadScript(url, callback) {
		var script = document.createElement("script");
		script.type = "text/javascript";

		if (script.readyState) { //IE
			script.onreadystatechange = function() {
				if (script.readyState === "loaded" || script.readyState === "complete") {
					script.onreadystatechange = null;
					callback();
				}
			};
		} else { //Others
			script.onload = function() {
				callback();
			};
		}
		script.src = url;
		document.getElementsByTagName('head')[0].appendChild(script);
	}

	function loadStylesheet(url) {
		var link = document.createElement("link");
		link.rel = "stylesheet";
		link.type = "text/css";
		link.href = url;
		document.getElementsByTagName("head")[0].appendChild(link);
	}

	function initPlugin($, host) {
		$(document).ready(function() {
			// for each widget, parse options in data-options attr (formatted like qstring), and call plugin
			$('.lwcw').each(function() {
				var $this			 = $(this);
				var option_str = $this.attr('data-options');

				// call iframe widget jquery plugin
				$this.lwWidget( $.extend({ host: host }, parseOptions(option_str)) );
			});
		});
	}

	function init() {
		var script			= document.getElementById('lw_lwcw'),
				option_str	= script.getAttribute('data-options'),
				liveurl_dir = '/live',
				load_styles = true;

		// use liveurl_dir set in options if it exists there
		if (option_str) {
			var opts = parseOptions(option_str);
			if (opts.liveurl_dir) {
				liveurl_dir = opts.liveurl_dir;
			}
			if (opts.exclude_styles) {
				load_styles = false;
			}
		}

		// return right away if script tag that loads this is not found
		if (!script || !script.src) return;

		// get host from script tag's src
		var host = script.src.match(/^\s*https?:\/\/[^/]+/i);
		if (host.length !== 1) return;
		host = host[0];

		var scripts = [
			'/livewhale/scripts/lib/date/formatter.js',
			'/livewhale/scripts/lib/date/timezone.js',
			'/livewhale/scripts/lib/date/user.js',
			'/livewhale/plugins/jquery/jquery.lw-widget.js'
		];
		var styles = [
			'/livewhale/theme/global/styles/widgets.css'
		];
		if (!isValidJQuery()) {
			scripts.unshift('/livewhale/thirdparty/jquery/jquery.no-conflict-footer.js');
			scripts.unshift('/livewhale/thirdparty/jquery/jquery.js');
			scripts.unshift('/livewhale/thirdparty/jquery/jquery.no-conflict-header.js');
		}
		var url = getAggregateUrl(host, liveurl_dir, 'js', scripts);
		var css_url = getAggregateUrl(host, liveurl_dir, 'css', styles);

		if (load_styles) {
			loadStylesheet(css_url);
		}
		loadScript(url, function() {
			var $ = livewhale.jQuery || jQuery;
			initPlugin(livewhale.jQuery || jQuery, host);

			// load randomize plugin if widget has lw_widget_randomized class
			$('body').on('widgetLoad.lw', function(e, $widget) {
				if ($widget.find('.lw_widget_randomized').length) {
					var randomize_scripts = [
						'%5Clivewhale%5Cthirdparty%5Cjqueryui%5Cjquery-ui.widget.js',
						'%5Clivewhale%5Cscripts%5Clwui%5Cjquery.lw-randomize.js'
					];
					var url = host + liveurl_dir + '/resource/js/' + randomize_scripts.join('/');

					loadScript(url, function() {
						// call randomize plugin
						$widget.find('.lw_widget_randomized').randomize();
					});
				}
			});
		});
	}

  // Multiple lwcw.js scripts are sometimes included in a page.
  // Make sure only one runs
  if (typeof window.LW_REMOTE_WIDGET_INITIALIZED === 'undefined') {
    window.LW_REMOTE_WIDGET_INITIALIZED = true;

    // run on document ready so site's existing jQuery, or LW jQuery, loads first
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', init);
    } else {
      init();
    }
  }
}());
