import Alamofire
import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(HttpNativePlugin)
public class HttpNativePlugin: CAPPlugin {
    @objc func initialize(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": value
        ])
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

        _headers.removeValue(forKey: "Content-Type");

        var encoder: ParameterEncoder = JSONParameterEncoder.default;
        if (contentType == "application/x-www-form-urlencoded") {
            encoder = URLEncodedFormParameterEncoder.default;
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
        loadCookies();
        AF.request(url, method: HTTPMethod(rawValue: method), parameters: parameters, encoder: encoder, headers: headers)
            .response { response in
                if let headerFields = response.response?.allHeaderFields as? [String: String], let url = response.request?.url {
                    let cookies = HTTPCookie.cookies(withResponseHeaderFields: headerFields, for: URL(string: "itspay.com.br")!)
                    var cookieArray = [[HTTPCookiePropertyKey: Any]]()
                    for cookie in cookies {
                       cookieArray.append(cookie.properties!)
                    }
                    UserDefaults.standard.set(cookieArray, forKey: "savedCookies")
                    UserDefaults.standard.synchronize()
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
                        print("Error: \(error)")
                        call.resolve([
                            "data": error
                        ])
                }
            }
    }
    
    func loadCookies() {
        guard let cookieArray = UserDefaults.standard.array(forKey: "savedCookies") as? [[HTTPCookiePropertyKey: Any]] else { return }
        for cookieProperties in cookieArray {
            if let cookie = HTTPCookie(properties: cookieProperties) {
                HTTPCookieStorage.shared.setCookie(cookie)
            }
        }
    }
}
