export interface HttpNativePlugin {
  doPost(options: { url: string, data: any, headers: any }): Promise<{ data: any }>;
  doGet(options: { url: string, params: any, headers: any }): Promise<{ data: any }>;
  initialize(options: { hostname: string, certPath: string }): Promise<{ data: any }>;
}
