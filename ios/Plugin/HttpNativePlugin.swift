import Alamofire
import Security
import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(HttpNativePlugin)
public class HttpNativePlugin: CAPPlugin {
    var session: Session?
    var timeoutInterval = 30
    @objc func initialize(_ call: CAPPluginCall) {
        self.timeoutInterval = call.getInt("timeout", 30)
        guard let certificateURL = Bundle.main.url(forResource: call.getString("certPath", ""), withExtension: nil),
              let certificateData = try? Data(contentsOf: certificateURL)
        else {
            call.reject("Falha SSL Pinning")
            return
        }
        let pinnedCertificates: [SecCertificate] = [SecCertificateCreateWithData(nil, certificateData as CFData)!]
        let serverTrustEvaluator = PinnedCertificatesTrustEvaluator(
            certificates: pinnedCertificates,
            acceptSelfSignedCertificates: true,
            performDefaultValidation: true,
            validateHost: true
        )
        let evaluators: [String: ServerTrustEvaluating] = [
            call.getString("hostname", ""): serverTrustEvaluator
        ]
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = TimeInterval(self.timeoutInterval)
        let serverTrustManager = ServerTrustManager(evaluators: evaluators)
        self.session = Session(configuration: configuration, serverTrustManager: serverTrustManager)
        call.resolve();
    }

    @objc func request(_ call: CAPPluginCall) {
        let url = call.getString("url") ?? ""
        let method = call.getString("method") ?? "POST";
        var data = call.getObject("data") ?? [:]
        if (data.isEmpty) {
            data = call.getObject("params") ?? [:]
        }

        var _headers = call.getObject("headers") ?? [:]

        let contentType = _headers["Content-Type"] as? String ?? "application/json"

        _headers.removeValue(forKey: "Content-Type")

        var encoder: ParameterEncoder = JSONParameterEncoder.default
        if (contentType == "application/x-www-form-urlencoded" || method == "GET") {
            encoder = URLEncodedFormParameterEncoder.default
        }

        var headers: HTTPHeaders = [];

        var parameters: [String: String] = [:];

        for (_, option) in data.enumerated() {
            if let value = option.value as? String {
                parameters[option.key] = value
            } else {
                parameters[option.key] = (option.value as? NSNumber)?.stringValue
            }
        }

        for (_, option) in _headers.enumerated() {
            headers.add(name: option.key, value: option.value as! String)
        }
        headers.add(name: "User-Agent", value: "\(deviceName()) \(deviceVersion()) \(CFNetworkVersion()) \(DarwinVersion())")
        let cookie = UserDefaults.standard.string(forKey: "savedCookies") ?? "";
        if (!cookie.isEmpty && !url.contains("oauth2") && !url.contains("auth")) {
            headers.add(name: "Cookie", value: cookie);
        }
        self.session?.request(url, method: HTTPMethod(rawValue: method), parameters: parameters, encoder: encoder, headers: headers)
            .validate(statusCode: 200..<300)
            .response { response in
                print ("response: \(response.debugDescription)")
                if let curlRequest = response.request?.debugDescription {
                    print("cURL command: \(curlRequest)")
                }
                if let headerFields = response.response?.allHeaderFields as? [String: String] {
                    let cookie = headerFields["Set-Cookie"];
                    if (cookie != nil && !(cookie?.isEmpty ?? true)) {
                        UserDefaults.standard.set(cookie    , forKey: "savedCookies")
                        UserDefaults.standard.synchronize()
                    }
                }
                switch response.result {
                case .success(let data):
                    if let stringValue = String(data: data!, encoding: .utf8) {
                        call.resolve([
                            "data": stringValue
                        ])
                    } else {
                        call.resolve([
                            "data": "{}"
                        ])
                    }
                case .failure(let error):
                    if let data = response.data, let errorBody = String(data: data, encoding: .utf8) {
                        call.reject(errorBody)
                    }
                    print("Error: \(error.localizedDescription)")
                }
            }
    }

    //eg. Darwin/16.3.0
    func DarwinVersion() -> String {
        var sysinfo = utsname()
        uname(&sysinfo)
        let dv = String(bytes: Data(bytes: &sysinfo.release, count: Int(_SYS_NAMELEN)), encoding: .ascii)!.trimmingCharacters(in: .controlCharacters)
        return "Darwin/\(dv)"
    }
    //eg. CFNetwork/808.3
    func CFNetworkVersion() -> String {
        let dictionary = Bundle(identifier: "com.apple.CFNetwork")?.infoDictionary!
        let version = dictionary?["CFBundleShortVersionString"] as! String
        return "CFNetwork/\(version)"
    }

    //eg. iOS/10_1
    func deviceVersion() -> String {
        let currentDevice = UIDevice.current
        return "\(currentDevice.systemName)/\(currentDevice.systemVersion)"
    }
    //eg. iPhone5,2
    func deviceName() -> String {
        var sysinfo = utsname()
        uname(&sysinfo)
        return String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)!.trimmingCharacters(in: .controlCharacters)
    }
}
