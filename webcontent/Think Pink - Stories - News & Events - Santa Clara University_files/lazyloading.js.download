const target = document.querySelectorAll(".lazySrcset, div.lazyBackground, .lazy");
  	const options = {
     // threshold: 1
        rootMargin: "500px"
		}
  	var observer = new IntersectionObserver((elements, observer) => {
    elements.forEach((element) => {
    if (element.isIntersecting) {                            // Checking if the element is intersecting or not.
        var img = element.target;
      	if (img.classList.contains("lazy")) {
            path = img.getAttribute('data-src');
            img.setAttribute('src',path);
      			img.removeAttribute('data-src');
            }
        if (img.classList.contains("lazyBackground")) {
            path = img.getAttribute('data-src');
      			img.style.backgroundImage = "url(\""+path+"\" )";
      			img.removeAttribute('data-src');   // Last change made
            }
        if (img.classList.contains("lazySrcset")) {
            img.classList.remove("lazySrcset");
      			var dpr = Math.round(window.devicePixelRatio);
      			var variants = [img.getAttribute('data-1'),img.getAttribute('data-2')];
            var srcset_1 = `${variants[1]} 767w, ${variants[0]} 967w`;
            var srcset_2 = `${variants[1]} 300w, ${variants[0]} 900w`;
            var srcset_3 = `${variants[1]} 300w, ${variants[0]} 900w`;
            var sizes_2="(max-width: 767px) 25vw, 100vw";
            var sizes_3="(max-width: 767px) 12vw, 100vw";

            if (dpr === 1) {
              img.setAttribute('srcset', srcset_1);
      				img.setAttribute('src', variants[0]);
              removeVar(img);
            }
            else if (dpr === 2) {
              img.setAttribute('sizes', sizes_2);
              img.setAttribute('srcset', srcset_2);
      				img.setAttribute('src', variants[0]);
              removeVar(img);
            }
            else {
              img.setAttribute('sizes', sizes_3);
              img.setAttribute('srcset', srcset_3);
      				img.setAttribute('src', variants[0]);
              removeVar(img);
            }
           // img.setAttribute('src', img.getAttribute('data-src'));
        };
        if (!img.complete) {                                 // Checking if the image is completely loaded or not.
            img.addEventListener('load', lazyImageLoad, false);
            img.addEventListener('error', lazyImageError, false);
        }
        else {
            lazyImageLoad()
        }
        function lazyImageLoad(e) {                          // If image is loaded, this function will be called.
            img.parentNode.classList.remove('preloader');
           // img.removeAttribute('data-src');
        }
        function lazyImageError(e) {                         // If there is an error loading the image, this function will be called.
            var parent = e.currentTarget.parentNode;
            parent.classList.remove('preloader');
            parent.classList.add('ImageError');
        }
        function removeVar(target) {
          target.removeAttribute('data-1');
          target.removeAttribute('data-2');
          target.removeAttribute('data-3');
        }
        observer.unobserve(img);                             // Elements are unobserved using this method.
        }
    });
  }, options);

		target.forEach (ele => {
    	observer.observe(ele);
  	})
