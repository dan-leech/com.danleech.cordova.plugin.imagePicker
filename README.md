# Cordova Instagram Image Picker plugin

Native Photo/Video Gallery for iOS and Android, inspired by [Terikon](https://github.com/terikon/cordova-plugin-photo-library)

Totally rewritten to native components for Android and iOS.


# Install
```
cordova plugin add https://github.com/dan-leech/com.danleech.cordova.plugin.imagePicker
```

###Android

Add cdvimagepicker protocol  to Content-Security-Policy, like this:

```
<meta http-equiv="Content-Security-Policy" content="default-src gap://ready file://* * 'self' 'unsafe-inline' 'unsafe-eval' data: gap: ws: cdvimagepicker: https://ssl.gstatic.com; script-src 'self' 'unsafe-inline' 'unsafe-eval' *; style-src 'self' 'unsafe-inline' *; img-src * data: blob: cdvimagepicker: https://firebasestorage.googleapis.com">
```

###iOS
Set swift 4.1 manually in xcode target build settings

#Usage

####Pick Images:
```js
  cordova.plugins.ImagePicker.getPictures(images => {
    // do something with images      
  },
  err => {
    console.log('ImagePicker error:', err)
  }, { 
    selectedImages: selectedImages, 
    limit: 5, 
    thumbWidth: 500, 
    thumbHeight: 500, 
    thumbQuality: 100 
  })
```

####Take image source:
##### Android
```js
'cdvimagepicker://thumbnail?photoId=' + encodeURIComponent(img.id + ';' + img.path) + '&width=500&height=500&quality=100&cropX=' + img.cropX + '&cropY=' + img.cropY + '&cropW=' + img.cropW + '&cropH=' + img.cropH
```

##### iOS
You need to get blob and use it in `createObjectURL`
Be careful with memory leak clean you url after using `revokeObjectURL`
```js
function processImages (oldImages, images) {
  oldImages.forEach(function (img) {
    if (img._objUrl) {
      URL.revokeObjectURL(img._objUrl)
      img._objUrl = null
      img._blob = null
    }
  })

  images.forEach(function (img) {
    if (img.imageData) {
      let byteArray = new Uint8Array(img.imageData)
      let blob = new Blob([byteArray], {type: img.mimeType})

      img._blob = blob
    }
    if (img._blob) {
      img._objUrl = URL.createObjectURL(img._blob)
    }
  })

  return images
}
```

####Get image data from iOS:
```js
cordova.plugins.ImagePicker.requestPermission(() => {
   items.forEach(item => {
     promises.push(new Promise((resolve, reject) => {
       cordova.plugins.ImagePicker.getThumbnail(dataUrl => {
         if (!dataUrl) reject(new Error('Data is empty'))
           item._blob = dataURLtoBlob(dataUrl)
           item.id = 'image'
           resolve()
         }, err => {
           console.log('cordova.plugins.ImagePicker.getThumbnail err', err)
           reject(err)
         }, {
           photoId: 'image;' + item.path,
           width: 500,
           quality: 100,
           fit: true
       })
     }))
       console.log('item', item)
   })

     Promise.all(promises).then(() => { 
       // show images
      })
     .catch(err => { console.log('process images error', err) })
   }, () => {
     alert('Need photo library permission')
   })
}

function dataURLtoBlob (dataUrl) {
  let input = dataUrl.split(',')[1]
  input = input.replace(/\s/g, '') // remove spaces

  // let byteString = atob(unescape(encodeURIComponent(input))) // encoded url
  let byteString = atob(input)
  let mimeString = dataUrl.split(',')[0].split(':')[1].split(';')[0]

  // write the bytes of the string to an ArrayBuffer
  let ab = new ArrayBuffer(byteString.length)

  // create a view into the buffer
  let ia = new Uint8Array(ab)

  // set the bytes of the buffer to the correct values
  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i)
  }

  return new Blob([ab], { type: mimeString })
}
```

####Select video:
```js
cordova.plugins.ImagePicker.getVideo(videos => {
  // do something with video
}, err => {
  console.log('ImagePicker error:', err)
}, { thumbWidth: 1920, thumbHeight: 1080, thumbQuality: 100 })
```

####Play video:

use https://github.com/dan-leech/com.danleech.cordova.plugin.videoplayer
```js
cordova.plugins.VideoPlayer.play(this.src)
```

####Process video before play on iOS:

#####Video thumbnail:
```js
cordova.plugins.ImagePicker.requestPermission(() => {
  items.forEach(item => {
    promises.push(new Promise((resolve, reject) => {
      cordova.plugins.ImagePicker.getThumbnail(dataUrl => {
        if (!dataUrl) reject(new Error('Data is empty'))
          item._blob = dataURLtoBlob(dataUrl)
          item.id = 'video'
          resolve()
        }, err => {
          console.log('cordova.plugins.ImagePicker.getThumbnail err', err)
          reject(err)
        }, {
          photoId: 'video;' + item.path,
          width: 1920,
          height: 1080,
          quality: 100
        })
    }))
    console.log('item', item)
  })

  Promise.all(promises).then(() => {
    // show thumbnail
  }).catch(err => {
    console.log('_checkSharedItems process images error', err)
  })
}, () => {
  alert('Need photo library permission')
})
```

####Get video Blob
#####Android
```js
'cdvimagepicker://photo?photoId=' + encodeURIComponent(payload.source.src.id + ';' + payload.source.src.path
```
#####iOS
```js
new Promise((resolve, reject) => {
  cordova.plugins.ImagePicker.getPhoto(dataUrl => {
    if (!dataUrl) reject(new Error('Data is empty'))
    resolve(dataURLtoBlob(dataUrl))
  }, err => {
    console.log('cordova.plugins.ImagePicker.getPhoto video err', err)
    reject(err)
  }, {
    photoId: payload.source.src.id + ';' + payload.source.src.path,
    cropX: payload.source.src.cropX,
    cropY: payload.source.src.cropY,
    cropW: payload.source.src.cropW,
    cropH: payload.source.src.cropH
  })
  }).then(blob => {
    console.log('Video blob', blob)
}))
```

# TODO

- Improve documentation.
- iOS switch orientation
- iOS crash on big videos

# References

Parts are based on

- https://github.com/terikon/cordova-plugin-photo-library
- https://github.com/esafirm/android-image-picker
- https://github.com/Yummypets/YPImagePicker
