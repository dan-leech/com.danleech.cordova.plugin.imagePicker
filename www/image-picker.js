/**
 * An Image Picker plugin for Cordova
 *
 * Developed by Daniil Kostin
 */

var ImagePicker = function() {

};

/*
*	success - success callback
*	fail - error callback
*	options
*		.maximumImagesCount - max images to be selected, defaults to 15. If this is set to 1,
*		                      upon selection of a single image, the plugin will return it.
*		.width - width to resize image to (if one of height/width is 0, will resize to fit the
*		         other while keeping aspect ratio, if both height and width are 0, the full size
*		         image will be returned)
*		.height - height to resize image to
*		.quality - quality of resized image, defaults to 100
*/
ImagePicker.prototype.getPictures = function(success, fail, options) {
	if (!options) {
		options = {};
	}

	var params = {
		maximumImagesCount: options.maximumImagesCount ? options.maximumImagesCount : 15,
		multipleMode: options.multipleMode !== undefined ? options.multipleMode : true,
		returnMode: options.returnMode,
        selectedImages: options.selectedImages,
        limit: options.limit,
		width: options.width ? options.width : 0,
		height: options.height ? options.height : 0,
		quality: options.quality ? options.quality : 100,
		// ios
    thumbWidth: options.thumbWidth ? options.thumbWidth : 100,
		thumbHeight: options.thumbHeight ? options.thumbHeight : 100,
		thumbQuality: options.thumbQuality ? options.thumbQuality : 100
	};

	return cordova.exec(success, fail, "ImagePicker", "getPictures", [params]);
};

ImagePicker.prototype.getVideo = function(success, fail, options) {
  if (!options) {
    options = {
      maximumImagesCount: options.maximumImagesCount ? options.maximumImagesCount : 1,
      multipleMode: options.multipleMode !== undefined ? options.multipleMode : false,
      returnMode: options.returnMode,
      selectedImages: options.selectedImages,
      limit: options.limit,
      width: options.width ? options.width : 0,
      height: options.height ? options.height : 0,
      quality: options.quality ? options.quality : 100,
      // ios
      thumbWidth: options.thumbWidth ? options.thumbWidth : 100,
      thumbHeight: options.thumbHeight ? options.thumbHeight : 100,
      thumbQuality: options.thumbQuality ? options.thumbQuality : 100
    };
  }

  return cordova.exec(success, fail, "ImagePicker", "getVideo", [options]);
};

ImagePicker.prototype.getThumbnail = function(success, fail, options) {
  if (!options) {
    options = {};
  }

  return cordova.exec(success, fail, "ImagePicker", "getThumbnail", [options]);
};

ImagePicker.prototype.getPhoto = function(success, fail, options) {
  if (!options) {
    options = {};
  }

  return cordova.exec(success, fail, "ImagePicker", "getPhoto", [options]);
};

ImagePicker.prototype.requestPermission = function(success, fail, options) {
  return cordova.exec(success, fail, "ImagePicker", "requestPermission", [{}]);
};


var imagePicker = new ImagePicker();

module.exports = imagePicker;
