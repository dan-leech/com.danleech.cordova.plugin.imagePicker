//
//  YPPhotoSaver.swift
//  YPImgePicker
//
//  Created by Sacha Durand Saint Omer on 10/11/16.
//  Copyright © 2016 Yummypets. All rights reserved.
//

import Foundation
import Photos

public class YPPhotoSaver {
    class func trySaveImage(_ image: UIImage, inAlbumNamed: String, completion: @escaping (PHAsset?)->()) {
        if PHPhotoLibrary.authorizationStatus() == .authorized {
            if let album = self.findAlbum(albumName: inAlbumNamed) {
                saveImage(image: image, album: album, completion: completion)
                return
            }
            createAlbum(albumName: inAlbumNamed) { album in
                if let album = album {
                    self.saveImage(image: image, album: album, completion: completion)
                }
                else {
                    assert(false, "Album is nil")
                }
            }
        }
    }

    fileprivate class func saveImage(image: UIImage, album: PHAssetCollection, completion: @escaping (PHAsset?)->()) {
        var placeholder: PHObjectPlaceholder?
        PHPhotoLibrary.shared().performChanges({
            // Request creating an asset from the image
            let createAssetRequest = PHAssetChangeRequest.creationRequestForAsset(from: image)
            // Request editing the album
            guard let albumChangeRequest = PHAssetCollectionChangeRequest(for: album) else {
                assert(false, "Album change request failed")
                return
            }
            // Get a placeholder for the new asset and add it to the album editing request
            guard let photoPlaceholder = createAssetRequest.placeholderForCreatedAsset else {
                assert(false, "Placeholder is nil")
                return
            }
            placeholder = photoPlaceholder
            let enumeration: NSArray = [placeholder!]
            albumChangeRequest.addAssets(enumeration)
        }, completionHandler: { success, error in
            guard let placeholder = placeholder else {
                assert(false, "Placeholder is nil")
                completion(nil)
                return
            }

            if success {
                let id: String = placeholder.localIdentifier
                let assets = PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil)
                completion(assets[0])
            }
            else {
                print(error)
                completion(nil)
            }
        })
    }

    fileprivate class func findAlbum(albumName: String) -> PHAssetCollection? {
        let fetchOptions = PHFetchOptions()
        fetchOptions.predicate = NSPredicate(format: "title = %@", albumName)
        let fetchResult = PHAssetCollection.fetchAssetCollections(with: .album, subtype: .albumRegular, options: fetchOptions)
        guard let photoAlbum = fetchResult.firstObject as? PHAssetCollection else {
            return nil
        }
        return photoAlbum
    }

    fileprivate class func createAlbum(albumName: String, completion: @escaping (PHAssetCollection?)->()) {
        var albumPlaceholder: PHObjectPlaceholder?
        PHPhotoLibrary.shared().performChanges({
            // Request creating an album with parameter name
            let createAlbumRequest = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: albumName)
            // Get a placeholder for the new album
            albumPlaceholder = createAlbumRequest.placeholderForCreatedAssetCollection
        }, completionHandler: { success, error in
            guard let placeholder = albumPlaceholder else {
                assert(false, "Album placeholder is nil")
                completion(nil)
                return
            }

            let fetchResult = PHAssetCollection.fetchAssetCollections(withLocalIdentifiers: [placeholder.localIdentifier], options: nil)
            guard let album = fetchResult.firstObject as? PHAssetCollection else {
                assert(false, "FetchResult has no PHAssetCollection")
                completion(nil)
                return
            }

            if success {
                completion(album)
            }
            else {
                print(error)
                completion(nil)
            }
        })
    }
}
