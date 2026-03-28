import Foundation

/// Postgres stores **kg** in `weight_kg`; the UI shows **lbs**.
///
/// **Legacy data:** Rows created before lbs→kg conversion on save may still have a **pound**
/// value stored in `weight_kg` (e.g. 190 for “190 lb”). Those will read ~2.2× too high until
/// corrected in Supabase, e.g. `UPDATE progress_check_ins SET weight_kg = weight_kg / 2.2046226218`
/// for affected rows (backup first; scope by `created_at` or `client_id`).
enum Club360Units {
    private static let lbsPerKg = 2.2046226218

    /// Converts stored kg → display pounds (whole lbs).
    static func displayPoundsFromKg(_ kg: Double?) -> String? {
        guard let kg else { return nil }
        let lbs = kg * lbsPerKg
        return "\(Int(lbs.rounded())) lbs"
    }

    /// User-entered pounds → kg for `weight_kg` columns.
    static func kgFromPounds(_ pounds: Double) -> Double {
        pounds / lbsPerKg
    }

    /// `clients.height_cm` → imperial label (matches Android `toFeetInches`).
    static func feetInchesLabel(fromCm cm: Int?) -> String? {
        guard let cm, cm > 0 else { return nil }
        let totalIn = Int((Double(cm) / 2.54).rounded())
        let ft = totalIn / 12
        let inch = totalIn % 12
        return "\(ft)′ \(inch)″"
    }
}
