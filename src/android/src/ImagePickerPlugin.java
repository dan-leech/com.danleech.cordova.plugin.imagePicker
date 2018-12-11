/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.danleech.cordova.plugin.imagePicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.JsonReader;

import com.danleech.cordova.plugin.imagePicker.features.ImagePickerActivity;
import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfig;
import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfigFactory;
import com.danleech.cordova.plugin.imagePicker.features.IpCons;
import com.danleech.cordova.plugin.imagePicker.features.ReturnMode;
import com.danleech.cordova.plugin.imagePicker.model.Image;

import static java.security.AccessController.getContext;

public class ImagePickerPlugin extends CordovaPlugin {
	public static String TAG = "ImagePicker";

	public static final String PHOTO_LIBRARY_PROTOCOL = "cdvimagepicker";

	public static final int DEFAULT_WIDTH = 512;
	public static final int DEFAULT_HEIGHT = 384;
	public static final double DEFAULT_QUALITY = 0.85;

	private CallbackContext callbackContext;
	private JSONObject params;

	private Context getContext() {
		return this.cordova.getActivity().getApplicationContext();
	}

	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		 this.callbackContext = callbackContext;
		 this.params = args.getJSONObject(0);
		if (action.equals("getPictures")) {
			Intent intent = new Intent(cordova.getActivity(), ImagePickerActivity.class);

//			int max = 20;
//			int desiredWidth = 0;
//			int desiredHeight = 0;
//			int quality = 100;
//			if (this.params.has("maximumImagesCount")) {
//				max = this.params.getInt("maximumImagesCount");
//			}
//			if (this.params.has("width")) {
//				desiredWidth = this.params.getInt("width");
//			}
//			if (this.params.has("height")) {
//				desiredHeight = this.params.getInt("height");
//			}
//			if (this.params.has("quality")) {
//				quality = this.params.getInt("quality");
//			}
//			intent.putExtra("MAX_IMAGES", max);
//			intent.putExtra("WIDTH", desiredWidth);
//			intent.putExtra("HEIGHT", desiredHeight);
//			intent.putExtra("QUALITY", quality);

			ImagePickerConfig config = ImagePickerConfigFactory.createDefault();
			if (this.params.has("multipleMode") && this.params.getBoolean("multipleMode")) {
				config.setMode(IpCons.MODE_MULTIPLE);
			} else {
				config.setMode(IpCons.MODE_SINGLE);
			}

			if (this.params.has("returnMode")) {
				switch (this.params.getString("returnMode")) {
					case "all":
						config.setReturnMode(ReturnMode.ALL);
						break;
					case "none":
						config.setReturnMode(ReturnMode.NONE);
						break;
					case "camera_only":
						config.setReturnMode(ReturnMode.CAMERA_ONLY);
						break;
					case "gallery_only":
						config.setReturnMode(ReturnMode.GALLERY_ONLY);
						break;
				}
			}

			if (this.params.has("selectedImages")) {
				ArrayList<Image> selectedImages = new ArrayList<>();
				JSONArray res = this.params.getJSONArray("selectedImages");
				for (int i = 0; i < res.length(); i++) {
					JSONObject resObj = res.getJSONObject(i);
					selectedImages.add(new Image(resObj.getLong("id"), resObj.getString("name"), resObj.getString("path"), resObj.getString("duration"), resObj.getBoolean("isVideo")));
				}

				config.setSelectedImages(selectedImages);
			}

			if (this.params.has("limit")) {
				config.setLimit(this.params.getInt("limit"));
			}

			intent.putExtra(ImagePickerConfig.class.getSimpleName(), config);
			if (this.cordova != null) {
				this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
			}
		}
		if (action.equals("getVideo")) {
			Intent intent = new Intent(cordova.getActivity(), ImagePickerActivity.class);

			ImagePickerConfig config = ImagePickerConfigFactory.createDefault();
			config.setOnlyVideo(true);

			if (this.params.has("multipleMode") && this.params.getBoolean("multipleMode")) {
				config.setMode(IpCons.MODE_MULTIPLE);
			} else {
				config.setMode(IpCons.MODE_SINGLE);
			}

			if (this.params.has("returnMode")) {
				switch (this.params.getString("returnMode")) {
					case "all":
						config.setReturnMode(ReturnMode.ALL);
						break;
					case "none":
						config.setReturnMode(ReturnMode.NONE);
						break;
					case "camera_only":
						config.setReturnMode(ReturnMode.CAMERA_ONLY);
						break;
					case "gallery_only":
						config.setReturnMode(ReturnMode.GALLERY_ONLY);
						break;
				}
			}

			if (this.params.has("selectedImages")) {
				ArrayList<Image> selectedImages = new ArrayList<>();
				JSONArray res = this.params.getJSONArray("selectedImages");
				for (int i = 0; i < res.length(); i++) {
					JSONObject resObj = res.getJSONObject(i);
					selectedImages.add(new Image(resObj.getLong("id"), resObj.getString("name"), resObj.getString("path"), resObj.getString("duration"), resObj.getBoolean("isVideo")));
				}

				config.setSelectedImages(selectedImages);
			}

			if (this.params.has("limit")) {
				config.setLimit(this.params.getInt("limit"));
			}

			intent.putExtra(ImagePickerConfig.class.getSimpleName(), config);
			if (this.cordova != null) {
				this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
			}
		}
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK && data != null) {
			ArrayList<Image> images = data.getParcelableArrayListExtra(IpCons.EXTRA_SELECTED_IMAGES);

			JSONArray res = new JSONArray();
			for (Image image:images) {
				JSONObject imageObj = new JSONObject();
				try {
					imageObj.put("id", image.getId());
					imageObj.put("name", image.getName());
					imageObj.put("path", image.getPath());
					imageObj.put("duration", image.getRawDuration());
					imageObj.put("size", image.getSize());
					imageObj.put("isVideo", image.isVideo());
					res.put(imageObj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			this.callbackContext.success(res);
		} else if (resultCode == Activity.RESULT_CANCELED && data != null) {
			String error = data.getStringExtra("ERRORMESSAGE");
			this.callbackContext.error(error);
		} else if (resultCode == Activity.RESULT_CANCELED) {
			JSONArray res = new JSONArray();
			this.callbackContext.success(res);
		} else {
			this.callbackContext.error("No images selected");
		}
	}

	@Override
	public Uri remapUri(Uri uri) {

		if (!PHOTO_LIBRARY_PROTOCOL.equals(uri.getScheme())) {
			return null;
		}
		return toPluginUri(uri);

	}

	@Override
	public CordovaResourceApi.OpenForReadResult handleOpenForRead(Uri uri) throws IOException {

		Uri origUri = fromPluginUri(uri);

		boolean isThumbnail = origUri.getHost().toLowerCase().equals("thumbnail") && origUri.getPath().isEmpty();
		boolean isPhoto = origUri.getHost().toLowerCase().equals("photo") && origUri.getPath().isEmpty();

		if (!isThumbnail && !isPhoto) {
			throw new FileNotFoundException("URI not supported by ImagePicker");
		}

		// format "imageid;imageurl;[swap]"
		String photoId = origUri.getQueryParameter("photoId");
		if (photoId == null || photoId.isEmpty()) {
			throw new FileNotFoundException("Missing 'photoId' query parameter");
		}

		PhotoLibraryService service = PhotoLibraryService.getInstance();

		if (isThumbnail) {

			String widthStr = origUri.getQueryParameter("width");
			int width;
			try {
				width = widthStr == null || widthStr.isEmpty() ? DEFAULT_WIDTH : Integer.parseInt(widthStr);
			} catch (NumberFormatException e) {
				throw new FileNotFoundException("Incorrect 'width' query parameter");
			}

			String heightStr = origUri.getQueryParameter("height");
			int height;
			try {
				height = heightStr == null || heightStr.isEmpty() ? DEFAULT_HEIGHT : Integer.parseInt(heightStr);
			} catch (NumberFormatException e) {
				throw new FileNotFoundException("Incorrect 'height' query parameter");
			}

			String qualityStr = origUri.getQueryParameter("quality");
			double quality;
			try {
				quality = qualityStr == null || qualityStr.isEmpty() ? DEFAULT_QUALITY : Double.parseDouble(qualityStr);
				if (quality > 1)
					quality = quality / 100.0;
			} catch (NumberFormatException e) {
				throw new FileNotFoundException("Incorrect 'quality' query parameter");
			}

			String fitStr = origUri.getQueryParameter("fit");
			boolean fit = false;
			if (fitStr != null) {
				fitStr = fitStr.toLowerCase();
				try {
					fit = fitStr.equals("true") || fitStr.equals("1") || fitStr.equals("on");
				} catch (NumberFormatException e) {
					throw new FileNotFoundException("Incorrect 'fit' query parameter");
				}
			}

			PhotoLibraryService.PictureData thumbnailData = service.getThumbnail(getContext(), photoId, width, height, quality, fit);

			if (thumbnailData == null) {
				throw new FileNotFoundException("Could not create thumbnail");
			}

			InputStream is = new ByteArrayInputStream(thumbnailData.bytes);

			return new CordovaResourceApi.OpenForReadResult(uri, is, thumbnailData.mimeType, is.available(), null);

		} else { // isPhoto == true

			PhotoLibraryService.PictureAsStream pictureAsStream = service.getPhotoAsStream(getContext(), photoId);
			InputStream is = pictureAsStream.getStream();

			return new CordovaResourceApi.OpenForReadResult(uri, is, pictureAsStream.getMimeType(), is.available(), null);

		}
	}
}
