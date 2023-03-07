import { WebPlugin } from '@capacitor/core';

import type { HttpNativePlugin } from './definitions';

export class HttpNativeWeb extends WebPlugin implements HttpNativePlugin {
  initialize(options: { hostname: string; certPath: string; }): Promise<{ data: any; }> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
  doPost(options: { url: string; data: any; headers: any }): Promise<{ data: any }> {
    console.log(options);
    throw new Error('Indisponível na web');
  }
  doGet(options: { url: string; params: any; headers: any }): Promise<{ data: any }> {
    console.log(options);
    throw new Error('Indisponível na web');
  }
}
