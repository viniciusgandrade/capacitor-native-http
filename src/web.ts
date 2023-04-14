import { WebPlugin } from '@capacitor/core';

import type { HttpNativePlugin, InitializeParams, RequestParams } from './definitions';

export class HttpNativeWeb extends WebPlugin implements HttpNativePlugin {
  initialize(_options: InitializeParams): Promise<{ data: any; }> {
    console.warn(_options);
    console.log('Ignorando web.');
    return new Promise((resolve => resolve({} as any)));
  }
  request(_options: RequestParams): Promise<{ data: any }> {
    console.warn(_options);
    return Promise.resolve({ data: undefined });
  }
}
