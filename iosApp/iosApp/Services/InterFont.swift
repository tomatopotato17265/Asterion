import SwiftUI

enum InterWeight: String {
    case regular = "Inter-Regular"
    case medium = "Inter-Medium"
    case semibold = "Inter-SemiBold"
    case bold = "Inter-Bold"
}

extension Font {
    static func inter(
        _ weight: InterWeight = .regular,
        size: CGFloat,
        relativeTo textStyle: Font.TextStyle = .body
    ) -> Font {
        .custom(weight.rawValue, size: size, relativeTo: textStyle)
    }
}
