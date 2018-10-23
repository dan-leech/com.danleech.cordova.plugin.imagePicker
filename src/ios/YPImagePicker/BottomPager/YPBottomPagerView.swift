//
//  YPBottomPagerView.swift
//  YPImagePicker
//
//  Created by Sacha DSO on 24/01/2018.
//  Copyright © 2016 Yummypets. All rights reserved.
//

import UIKit
import Stevia

final class YPBottomPagerView: UIView {

    var header = YPPagerMenu()
    var _scrollView = UIScrollView()

    convenience init() {
        self.init(frame: .zero)
        backgroundColor = UIColor(red: 66, green: 66, blue: 66, alpha: 1)

        sv(
            _scrollView,
            header
        )

        layout(
            0,
            |_scrollView|,
            0,
            |header| ~ 44
        )

        if #available(iOS 11.0, *) {
            header.Bottom == safeAreaLayoutGuide.Bottom
        } else {
            header.bottom(0)
        }

        clipsToBounds = false
        _scrollView.clipsToBounds = false
        _scrollView.isPagingEnabled = true
        _scrollView.showsHorizontalScrollIndicator = false
        _scrollView.scrollsToTop = false
        _scrollView.bounces = false
    }
}
