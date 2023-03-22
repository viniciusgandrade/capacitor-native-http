import { WebPlugin } from '@capacitor/core';

import type { HttpNativePlugin } from './definitions';

export class HttpNativeWeb extends WebPlugin implements HttpNativePlugin {
  initialize(_options: { hostname: string[]; certPath: string; timeout: number; }): Promise<{ data: any; }> {
    console.log('Ignorando web.');
    return new Promise((resolve => resolve({} as any)));
  }
  request(_options: { method: string, url: string; params?: any; data?: any; headers?: any }): Promise<{ data: any }> {
    return Promise.resolve({ data: undefined });
  }
}
