export interface HttpNativePlugin {
  doPost(options: { url: string, data: any, headers: any }): Promise<{ data: any }>;
}
