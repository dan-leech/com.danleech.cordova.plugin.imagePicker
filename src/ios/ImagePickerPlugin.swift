import Foundation
import UIKit
import AVFoundation
import AVKit
import Photos

@objc(ImagePickerPlugin) class ImagePickerPlugin : CDVPlugin {
    var selectedItems = [YPMediaItem]()

    let selectedImageV = UIImageView()
    let pickButton = UIButton()
    let resultsButton = UIButton()

    lazy var concurrentQueue: DispatchQueue = DispatchQueue(label: "photo-library.queue.plugin", qos: DispatchQoS.utility, attributes: [.concurrent])

    lazy var operationQueue: OperationQueue = {
        var queue = OperationQueue()
        queue.name = "PhotoLibrary Protocol Queue"
        queue.qualityOfService = .background
        queue.maxConcurrentOperationCount = 4
        return queue
    }()

    override func pluginInitialize() {

        // Do not call PhotoLibraryService here, as it will cause permission prompt to appear on app start.
        URLProtocol.registerClass(PhotoLibraryProtocol.self)

    }

    override func onMemoryWarning() {
        // self.service.stopCaching()
        NSLog("-- MEMORY WARNING --")
    }

    private func YPGeneralConfig() -> YPImagePickerConfiguration {
        var config = YPImagePickerConfiguration()

        /* Uncomment and play around with the configuration 👨‍🔬 🚀 */

        /* Set this to true if you want to force the  library output to be a squared image. Defaults to false */
        config.library.onlySquare = true

        /* Set this to true if you want to force the camera output to be a squared image. Defaults to true */
        // config.onlySquareImagesFromCamera = false

        /* Ex: cappedTo:1024 will make sure images from the library or the camera will be
         resized to fit in a 1024x1024 box. Defaults to original image size. */
        // config.targetImageSize = .cappedTo(size: 1024)

        /* Choose what media types are available in the library. Defaults to `.photo` */
        config.library.mediaType = .photo

        /* Enables selecting the front camera by default, useful for avatars. Defaults to false */
        // config.usesFrontCamera = true

        /* Adds a Filter step in the photo taking process. Defaults to true */
        config.showsFilters = false

        /* Manage filters by yourself */
        //        config.filters = [YPFilter(name: "Mono", coreImageFilterName: "CIPhotoEffectMono"),
        //                          YPFilter(name: "Normal", coreImageFilterName: "")]
        //        config.filters.remove(at: 1)
        //        config.filters.insert(YPFilter(name: "Blur", coreImageFilterName: "CIBoxBlur"), at: 1)

        /* Enables you to opt out from saving new (or old but filtered) images to the
         user's photo library. Defaults to true. */
        config.shouldSaveNewPicturesToAlbum = true

        /* Choose the videoCompression. Defaults to AVAssetExportPresetHighestQuality */
        config.video.compression = AVAssetExportPresetMediumQuality

        /* Defines the name of the album when saving pictures in the user's photo library.
         In general that would be your App name. Defaults to "DefaultYPImagePickerAlbumName" */
        // config.albumName = "ThisIsMyAlbum"

        /* Defines which screen is shown at launch. Video mode will only work if `showsVideo = true`.
         Default value is `.photo` */
        config.startOnScreen = .library

        /* Defines which screens are shown at launch, and their order.
         Default value is `[.library, .photo]` */
        config.screens = [.library, .photo]

        /* Can forbid the items with very big height with this property */
        //        config.library.minWidthForItem = UIScreen.main.bounds.width * 0.8

        /* Defines the time limit for recording videos.
         Default is 30 seconds. */
        // config.video.recordingTimeLimit = 5.0

        /* Defines the time limit for videos from the library.
         Defaults to 60 seconds. */
        config.video.libraryTimeLimit = 5 * 60.0

        /* Adds a Crop step in the photo taking process, after filters. Defaults to .none */
        // config.showsCrop = .rectangle(ratio: (16/9))

        /* Defines the overlay view for the camera. Defaults to UIView(). */
        //        let overlayView = UIView()
        //        overlayView.backgroundColor = .red
        //        overlayView.alpha = 0.3
        //        config.overlayView = overlayView

        /* Customize wordings */
        config.wordings.libraryTitle = "Gallery"

        /* Defines if the status bar should be hidden when showing the picker. Default is true */
        config.hidesStatusBar = true

        config.library.maxNumberOfItems = 1

        /* Disable scroll to change between mode */
        // config.isScrollToChangeModesEnabled = false
        //        config.library.minNumberOfItems = 2

        /* Skip selection gallery after multiple selections */
        config.library.skipSelectionsGallery = true

        return config
    }

    private func processYPMediaItems(items: [YPMediaItem], width: Float, height: Float, quality: Float, completion: @escaping (NSMutableArray)->()) {
        let result = NSMutableArray()
        let dispatchGroup = DispatchGroup()

        for mediaItem: YPMediaItem in items {
            switch mediaItem {
            case .photo(let photo):
                dispatchGroup.enter()
                if photo.asset == nil {
                    print("Photo asset is nil!")
                    dispatchGroup.leave()
                    continue
                }

                let imageData = PhotoLibraryService.image2PictureData(photo.image.resized(to: CGSize(width: CGFloat(width), height: CGFloat(height)))!, quality: quality)

                let byteArray = [UInt8](imageData!.data)

                let item = [
                    "id": photo.asset?.localIdentifier,
                    "name": photo.asset?.fileName, // originalFilename is much slower
                    "path": "",
                    "duration": "",
                    "size": "0",
                    "cropX": photo.cropRect?.origin.x,
                    "cropY": photo.cropRect?.origin.y,
                    "cropW": photo.cropRect?.width,
                    "cropH": photo.cropRect?.height,
                    "restoreZoom": photo.zoomScale,
                    "restoreOffsetX": photo.contentOffset?.x,
                    "restoreOffsetY": photo.contentOffset?.y,
                    "isVideo": false,
                    "mimeType": imageData?.mimeType,
                    "imageData": byteArray
                ] as [AnyHashable : Any]

                result.add(item)
                dispatchGroup.leave()
            case .video(let video):
                dispatchGroup.enter()
                if video.asset == nil && !video.fromCamera {
                    print("Video asset is nil!")
                    dispatchGroup.leave()
                    continue
                }

                if (video.fromCamera) {
                    let asset = AVURLAsset(url: video.url)
                    var sizeOnDisk: Int64 = 0
                    var duration: Int64 = 0

                    do {
                        let attr = try FileManager.default.attributesOfItem(atPath: video.url.path)
                        sizeOnDisk = attr[FileAttributeKey.size] as! Int64
                    } catch {
                        print("Error: \(error)")
                    }

                    duration = Int64(asset.duration.seconds)

                    let imageData = PhotoLibraryService.image2PictureData(video.thumbnail.resized(to: CGSize(width: CGFloat(width), height: CGFloat(height)))!, quality: quality)

                    let byteArray = [UInt8](imageData!.data)

                    let item = [
                        "id": "",
                        "name": "", // no filename for captured video
                        "path": video.url.path,
                        "duration": String(duration),
                        "size": String(sizeOnDisk),
                        "cropX": "",
                        "cropY": "",
                        "cropW": "",
                        "cropH": "",
                        "restoreZoom": "",
                        "restoreOffsetX": "",
                        "restoreOffsetY": "",
                        "isVideo": true,
                        "mimeType": imageData?.mimeType,
                        "imageData": byteArray
                        ] as [AnyHashable : Any]

                    result.add(item)
                    dispatchGroup.leave()
                } else {
                    let options: PHVideoRequestOptions = PHVideoRequestOptions()
                    options.version = .original
                    PHImageManager.default().requestAVAsset(forVideo: video.asset!, options: options, resultHandler: {(asset: AVAsset?, audioMix: AVAudioMix?, info: [AnyHashable : Any]?) -> Void in
                        if let urlAsset = asset as? AVURLAsset {
                            let localVideoUrl: URL = urlAsset.url as URL

                            var sizeOnDisk: Int64 = 0
                            var duration: Int64 = 0

                            do {
                                let attr = try FileManager.default.attributesOfItem(atPath: localVideoUrl.path)
                                sizeOnDisk = attr[FileAttributeKey.size] as! Int64
                            } catch {
                                print("Error: \(error)")
                            }

                            if let d = video.asset?.duration {
                                duration = Int64(d)
                            }

                            let imageData = PhotoLibraryService.image2PictureData(video.thumbnail.resized(to: CGSize(width: CGFloat(width), height: CGFloat(height)))!, quality: quality)

                            let byteArray = [UInt8](imageData!.data)

                            let item = [
                                "id": video.asset?.localIdentifier,
                                "name": video.asset?.fileName, // originalFilename is much slower
                                "path": localVideoUrl.path,
                                "duration": String(duration),
                                "size": String(sizeOnDisk),
                                "cropX": video.cropRect?.origin.x,
                                "cropY": video.cropRect?.origin.y,
                                "cropW": video.cropRect?.width,
                                "cropH": video.cropRect?.height,
                                "restoreZoom": video.zoomScale,
                                "restoreOffsetX": video.contentOffset?.x,
                                "restoreOffsetY": video.contentOffset?.y,
                                "isVideo": true,
                                "mimeType": imageData?.mimeType,
                                "imageData": byteArray
                                ] as [AnyHashable : Any]

                            result.add(item)
                            dispatchGroup.leave()
                        } else {
                            print("Video file not found by asset")
                            dispatchGroup.leave()
                        }
                    })
                }
            }
        }

        dispatchGroup.notify(queue: .main) {
            completion(result)
        }
    }

    // Basic methods
    @objc(getPictures:)
    func getPictures(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! NSDictionary
        let limit = options["limit"] as? Int
        let selectedImages = options["selectedImages"] as? [NSDictionary]

        let width = options["thumbWidth"] as? Float ?? 100.0
        let height = options["thumbHeight"] as? Float ?? 100.0
        let quality = options["thumbQuality"] as? Float ?? 100.0

        var config = self.YPGeneralConfig()
        config.library.mediaType = .photo
        config.screens = [.library, .photo]

        if limit != nil {
            config.library.maxNumberOfItems = limit!
        }

        if selectedImages != nil {
            config.library.selectedImages = selectedImages!
        }

        let picker = YPImagePicker(configuration: config)

        /* Multiple media implementation */
        picker.didFinishPicking { [unowned picker] items, cancelled in

            if cancelled {
                print("Picker was canceled")
                picker.dismiss(animated: true, completion: nil)
                return
            }
            _ = items.map {
                print("🧀 \($0)")
            }

            self.processYPMediaItems(items: items, width: width, height: height, quality: quality) { result in
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result.compactMap {
                    $0 as? [AnyHashable: Any]
                })
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)

                picker.dismiss(animated: true, completion: nil)
            }
        }

        self.viewController.present(picker, animated: true, completion: nil)
    }

    @objc(getVideo:)
    func getVideo(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! NSDictionary
        let limit = options["limit"] as? Int

        let width = options["thumbWidth"] as? Float ?? 100.0
        let height = options["thumbHeight"] as? Float ?? 100.0
        let quality = options["thumbQuality"] as? Float ?? 100.0

        var config = self.YPGeneralConfig()
        config.library.mediaType = .video
        config.screens = [.library, .video]

        if limit != nil {
           config.library.maxNumberOfItems = limit!
        }

        let picker = YPImagePicker(configuration: config)

        /* Multiple media implementation */
        picker.didFinishPicking { [unowned picker] items, cancelled in

            if cancelled {
               print("Picker was canceled")
               picker.dismiss(animated: true, completion: nil)
                return
            }
            _ = items.map {
                print("🧀 \($0)")
            }

            self.processYPMediaItems(items: items, width: width, height: height, quality: quality) { result in
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result.compactMap {
                    $0 as? [AnyHashable: Any]
                })
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)

                picker.dismiss(animated: true, completion: nil)
            }
        }

        self.viewController.present(picker, animated: true, completion: nil)
    }

    private func dataToDataUrl(imageData: Data, mimeType: String) -> String? {
        return String(format: "data:%@;base64,%@", mimeType, imageData.base64EncodedString(options: .lineLength64Characters))
    }

    // Async beacause will prompt permission if .notDetermined
    // and ask custom popup if denied.
    func checkPermissionToAccessPhotoLibrary(block: @escaping (Bool) -> Void) {
        // Only initialize picker if photo permission is Allowed by user.
        let status = PHPhotoLibrary.authorizationStatus()
        switch status {
        case .authorized:
            block(true)
        case .restricted, .denied:
            let popup = YPPermissionDeniedPopup()
            let alert = popup.popup(cancelBlock: {
                block(false)
            })
            self.viewController.present(alert, animated: true, completion: nil)
        case .notDetermined:
            // Show permission popup and get new status
            PHPhotoLibrary.requestAuthorization { s in
                DispatchQueue.main.async {
                    block(s == .authorized)
                }
            }
        }
    }

    @objc(requestPermission:)
    func requestPermission(_ command: CDVInvokedUrlCommand) {
        checkPermissionToAccessPhotoLibrary { hasPermission in
            var pluginResult: CDVPluginResult
            if hasPermission {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "success")
            } else {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: PhotoLibraryService.PERMISSION_ERROR)
            }
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(getThumbnail:)
    func getThumbnail(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! NSDictionary

        let photoId = options["photoId"] as? String
        if photoId == nil {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Missing 'photoId' parameter")
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            return
        }

        if !PhotoLibraryService.hasPermission() {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: PhotoLibraryService.PERMISSION_ERROR)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            return
        }

        let service = PhotoLibraryService.instance

        let width = options["width"] as? Int ?? 512
        let height = options["height"] as? Int ?? 384
        let fit = options["fit"] as? Bool ?? false
        var quality = options["quality"] as? Float ?? 0.5
        if (quality > 1) {
            quality = quality / 100
        }

        let cropX = options["cropX"] as? CGFloat ?? 0.0
        let cropY = options["cropY"] as? CGFloat ?? 0.0
        let cropW = options["cropW"] as? CGFloat ?? 1.0
        let cropH = options["cropH"] as? CGFloat ?? 1.0

        let cropRect = CGRect(x: cropX, y: cropY, width: cropW, height: cropH)

        operationQueue.addOperation {
            service.getThumbnail(photoId!, thumbnailWidth: width, thumbnailHeight: fit == true ? width : height, quality: quality, cropRect: cropRect) { (imageData) in
                if (imageData == nil) {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: PhotoLibraryService.PERMISSION_ERROR)
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
                    return
                }

                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: self.dataToDataUrl(imageData: imageData!.data, mimeType: imageData!.mimeType))
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    @objc(getPhoto:)
    func getPhoto(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! NSDictionary

        let photoId = options["photoId"] as? String
        if photoId == nil {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Missing 'photoId' parameter")
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            return
        }

        if !PhotoLibraryService.hasPermission() {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: PhotoLibraryService.PERMISSION_ERROR)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            return
        }

        let service = PhotoLibraryService.instance

        let width = options["width"] as? Int ?? 512
        let height = options["height"] as? Int ?? 384
        let fit = options["fit"] as? Bool ?? false
        let quality = options["quality"] as? Float ?? 0.5

        let cropX = options["cropX"] as? CGFloat ?? 0.0
        let cropY = options["cropY"] as? CGFloat ?? 0.0
        let cropW = options["cropW"] as? CGFloat ?? 1.0
        let cropH = options["cropH"] as? CGFloat ?? 1.0

        let cropRect = CGRect(x: cropX, y: cropY, width: cropW, height: cropH)

        operationQueue.addOperation {
            service.getPhoto(photoId!, width: width, height: fit == true ? width : height, quality: quality, cropRect: cropRect) { (imageData) in
                if (imageData == nil) {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: PhotoLibraryService.PERMISSION_ERROR)
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
                    return
                }
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: self.dataToDataUrl(imageData: imageData!.data, mimeType: imageData!.mimeType))
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func isAuthorized(_ command: CDVInvokedUrlCommand) {
        concurrentQueue.async {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: PhotoLibraryService.hasPermission())
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        }
    }

    func stopCaching(_ command: CDVInvokedUrlCommand) {

        let service = PhotoLibraryService.instance

        service.stopCaching()

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId	)

    }

    func requestAuthorization(_ command: CDVInvokedUrlCommand) {

        let service = PhotoLibraryService.instance

        service.requestAuthorization({
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId	)
        }, failure: { (err) in
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: err)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId	)
        })

    }
}
