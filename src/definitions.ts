export interface InitializeParams {
  hostname?: string[];
  certPath?: string;
  timeout?: number;
  addInterceptor?: string;
  receivedInterceptor?:string;
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
}
