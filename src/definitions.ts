export interface InitializeParams {
  hostname?: string;
  certPath?: string;
  timeout?: number;
  certPassMtls?: string;
  certPathMtls?: string;
  saveLoginCookie?: boolean;
}

export interface RequestParams {
  method: string;
  url: string;
  params?: any;
  data?:any;
  headers?:any;
}

export interface HttpNativePlugin {
  request(options: RequestParams): Promise<{ data: any }>;
  initialize(options: InitializeParams): Promise<{ data: any }>;
  clearCookie(): Promise<any>;
}
