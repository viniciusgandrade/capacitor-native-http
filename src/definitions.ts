export interface HttpNativePlugin {
  request(options: { method: string, url: string, params?: any, data?:any, headers?:any }): Promise<{ data: any }>;
  initialize(options: { hostname: string, certPath: string }): Promise<{ data: any }>;
}
