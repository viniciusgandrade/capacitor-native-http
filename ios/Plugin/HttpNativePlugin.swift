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
    var saveLoginCookie = false
    var session: Session?
    var timeoutInterval = 30
    var certMtlsPath = ""
    var certPassMtls = ""
    @objc func initialize(_ call: CAPPluginCall) {
        self.timeoutInterval = call.getInt("timeout", 30)
        let host = call.getString("hostname", "")
        self.certMtlsPath = call.getString("certPathMtls", "")
        self.certPassMtls = call.getString("certPassMtls", "")
        self.saveLoginCookie = call.getBool("saveLoginCookie", false)
        let fileManager = FileManager.default
        do {
            guard let directoryPath = Bundle.main.url(forResource: call.getString("certPath"), withExtension: nil) else {
                print("Directory 'certificates' not found in bundle.")
                return
            }
            let directoryURL = URL(fileURLWithPath: directoryPath.path)
            let contents = try fileManager.contentsOfDirectory(at: directoryURL, includingPropertiesForKeys: nil, options: [])

            var pinnedCertificates: [SecCertificate] = []

            for fileURL in contents {
                if fileURL.pathExtension == "cer" {
                    if let certificateData = try? Data(contentsOf: fileURL) {
                        if let certificate = SecCertificateCreateWithData(nil, certificateData as CFData) {
                            pinnedCertificates.append(certificate)
                        }
                    }
                }
            }

            if !pinnedCertificates.isEmpty {
                let serverTrustEvaluator = PinnedCertificatesTrustEvaluator(
                    certificates: pinnedCertificates,
                    acceptSelfSignedCertificates: true,
                    performDefaultValidation: true,
                    validateHost: true
                )

                UserDefaults.standard.set("", forKey: "savedCookies")
                UserDefaults.standard.synchronize()
                let configuration: URLSessionConfiguration = URLSessionConfiguration.default
                configuration.timeoutIntervalForRequest = TimeInterval(self.timeoutInterval)

                let evaluators: [String: ServerTrustEvaluating] = [
                    host: serverTrustEvaluator,
                    "": DefaultTrustEvaluator()
                ]

                let manager = WildcardServerTrustPolicyManager(evaluators: evaluators)
                self.session = Session(configuration: configuration, serverTrustManager: manager)
                call.resolve();
            } else {
                print("No .cer files found in the directory.")
            }
        } catch {
            print("Error reading directory: \(error)")
        }
    }

    @objc func clearCookie(_ call: CAPPluginCall) {
        UserDefaults.standard.set("", forKey: "savedCookies")
        UserDefaults.standard.synchronize()
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
        let contentTypeTemp = _headers["contentType"] as? String ?? ""

        _headers.removeValue(forKey: "Content-Type")

        var encoder: ParameterEncoder = JSONParameterEncoder.default
        if (contentType == "application/x-www-form-urlencoded" || method == "GET") {
            encoder = URLEncodedFormParameterEncoder.default
        }

        var headers: HTTPHeaders = [];

        for (_, option) in _headers.enumerated() {
            headers.add(name: option.key, value: option.value as! String)
        }
        headers.add(name: "User-Agent", value: "App/\(getAppVersion()) (\(deviceName()); \(deviceVersion()); Scale/\(getScreenScale()))")
        let cookie = UserDefaults.standard.string(forKey: "savedCookies") ?? "";
        if (!cookie.isEmpty && !url.contains("oauth2") && !url.contains("auth")) {
            headers.add(name: "Cookie", value: cookie);
        }

        var request: DataRequest?;
        let url1 = URL(string: url)!
        if (method == "GET") {
            var parameters: [String: String] = [:];

            for (_, option) in data.enumerated() {
                if let value = option.value as? String {
                    parameters[option.key] = value
                } else {
                    parameters[option.key] = (option.value as? NSNumber)?.stringValue
                }
            }
            request = self.session?.request(url, method: HTTPMethod(rawValue: method), parameters: parameters, encoder: encoder, headers: headers);
        } else {
            if (contentTypeTemp == "multipart/form-data"){
                request = AF.upload(multipartFormData: { multipartFormData in
                // Add parameters
                for (key, value) in data {
                    if key == "file" {
                        continue;
                    }
                    if let stringValue = value as? String {
                        multipartFormData.append(Data(stringValue.utf8), withName: key)
                    } else if let numberValue = value as? NSNumber {
                        if CFGetTypeID(numberValue) == CFBooleanGetTypeID() {
                            multipartFormData.append(Data((numberValue.boolValue ? "true" : "false").utf8), withName: key)
                        } else {
                            multipartFormData.append(Data(numberValue.stringValue.utf8), withName: key)
                        }
                    }
                }

                // Add file data
                    if let base64File = data["file"] as? String,
                       let fileData = Data(base64Encoded: base64File, options: .ignoreUnknownCharacters),
                       let fileName = data["nome"] as? String,
                       let formato = data["formato"] as? String {
                        let mimeType = (formato == "png" ? "image" : "application") + "/\(formato)"
                        multipartFormData.append(fileData, withName: "file", fileName: fileName, mimeType: mimeType)
                    }
            }, to: url1, method: HTTPMethod(rawValue: method), headers: headers)
            } else {
                guard let jsonData = try? JSONSerialization.data(withJSONObject: data, options: []) else {
                    call.reject("JSON inválido")
                    return;
                }
                var urlRequest = URLRequest(url: url1)
                urlRequest.httpMethod = method
                urlRequest.headers = headers
                urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
                urlRequest.httpBody = jsonData

                request = self.session?.request(urlRequest)
            }
        }
        if (!self.certMtlsPath.isEmpty) {
            if let credential = createPKCS12Credential(certPath: self.certMtlsPath, certPass: self.certPassMtls) {
                request?.authenticate(with: credential)
            }
        }

        request?
            .validate(statusCode: 200..<300)
            .response { response in
                print ("response: \(response.debugDescription)")
                do {
                    if let curlRequest = response.request?.debugDescription {
                        print("cURL command: \(curlRequest)")
                    }
                    if let headerFields = response.response?.allHeaderFields as? [String: String] {
                        let cookie = headerFields["Set-Cookie"];
                        if (self.saveLoginCookie && url.contains("/auth")) {
                            UserDefaults.standard.set(cookie    , forKey: "savedCookies")
                            UserDefaults.standard.synchronize()
                        } else if (!self.saveLoginCookie) {
                            if (cookie != nil && !(cookie?.isEmpty ?? true)) {
                                UserDefaults.standard.set(cookie    , forKey: "savedCookies")
                                UserDefaults.standard.synchronize()
                            }
                        }
                    }
                    switch response.result {
                    case .success(let data):
                        if let data = data {
                            let contentType = response.response?.mimeType
                            var responseData: String

                            if contentType?.contains("application/json") == true || contentType?.contains("text/") == true {
                                responseData = String(data: data, encoding: .utf8) ?? "{}"
                            } else {
                                responseData = data.base64EncodedString()
                            }

                            var headersDict = [String: String]()
                                if let headers = response.response?.allHeaderFields as? [String: String] {
                                    headersDict = headers
                                }

                            let headersJsonString = (try? JSONSerialization.data(withJSONObject: headersDict, options: []))
                                       .flatMap { String(data: $0, encoding: .utf8) } ?? "{}"


                                call.resolve([
                                    "data": responseData,
                                    "headers": headersJsonString
                                ])
                        } else {
                            call.resolve([
                                "data": "{}"
                            ])
                        }
                    case .failure(let error):
                        if let statusCode = response.response?.statusCode {
                            if let data = response.data, let errorBody = try JSONSerialization.jsonObject(with: data) as? [String: Any], let err = try? JSONSerialization.data(withJSONObject: ["data": errorBody, "statusCode": statusCode]), let jsonString = String(data: err, encoding: .utf8) {
                                call.reject(jsonString)
                            } else {
                                print("Validation error: Unacceptable status code \(statusCode)")
                                self.mountErrorResponse(msg: "Erro ao processar requisição.", statusCode: statusCode, call: call)
                            }
                        } else {
                            var status = 403
                            var msg = "Expired"
                            if (error.localizedDescription.contains("timed out")) {
                                status = 400
                                msg = "Erro ao processar requisição"
                            }
                            print("Request error1: \(error.localizedDescription)")
                            self.mountErrorResponse(msg: msg, statusCode: status, call: call)
                        }
                    }
                } catch let error {
                    print("Request error2: \(error.localizedDescription)")
                    self.mountErrorResponse(msg: "Erro ao processar requisição", statusCode: 400, call: call)
                }
            }
    }

    func mountErrorResponse(msg: String, statusCode: Int, call: CAPPluginCall) {
        var ret = [String: Any]()
        ret["msg"] = msg
        if msg.contains("SecTrust") {
            ret["msg"] = "Erro ao processar a requisição."
        }
        ret["status"] = "ko"
        var jsonObject = [String: Any]()
        jsonObject["data"] = ret
        jsonObject["statusCode"] = statusCode
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: jsonObject)
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                call.reject(jsonString)
            }
        } catch {
            // Handle JSON serialization error
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
        return "\(currentDevice.systemName) \(currentDevice.systemVersion)"
    }
    //eg. iPhone5,2
    func deviceName() -> String {
        //        var sysinfo = utsname()
        //        uname(&sysinfo)
        //        return String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)!.trimmingCharacters(in: .controlCharacters)
        return UIDevice.current.model
    }

    func getScreenScale() -> String {
        return String(format: "%.2f", UIScreen.main.scale)
    }

    func getAppVersion() -> String {
        return Bundle.main.infoDictionary?["CFBundleShortVersionString"] as! String
    }
}

class WildcardServerTrustPolicyManager: ServerTrustManager {
    override func serverTrustEvaluator(forHost host: String) throws -> ServerTrustEvaluating? {
        if let policy = evaluators[host] {
            return policy
        }
        var domainComponents = host.split(separator: ".")
        if domainComponents.count > 2 {
            domainComponents[0] = "*"
            let wildcardHost = domainComponents.joined(separator: ".")
            return evaluators[wildcardHost]
        }
        return nil
    }
}

extension String: LocalizedError {
    public var errorDescription: String? { return self }
}

func createPKCS12Credential(certPath: String, certPass: String) -> URLCredential? {
    let certificatePath = Bundle.main.path(forResource: certPath, ofType: nil)
    let certificateData = try? Data(contentsOf: URL(fileURLWithPath: certificatePath!))

    let options: [String: Any] = [
        kSecImportExportPassphrase as String: certPass
    ]

    var importedItems: CFArray?
    let status = SecPKCS12Import(certificateData! as NSData, options as CFDictionary, &importedItems)

    if status == errSecSuccess, let itemsArray = importedItems as? Array<Dictionary<String, Any>>, let firstItem = itemsArray.first {
        let identityKey = kSecImportItemIdentity as String
        if let identity = firstItem[identityKey] {
            let credential = URLCredential(identity: identity as! SecIdentity, certificates: nil, persistence: .forSession)
            return credential
        }
    }

    return nil
}
