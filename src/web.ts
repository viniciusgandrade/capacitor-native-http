import { WebPlugin } from '@capacitor/core';

import type { HttpNativePlugin } from './definitions';

export class HttpNativeWeb extends WebPlugin implements HttpNativePlugin {
  doPost(options: { url: string; data: any; headers: any }): Promise<{ data: any }> {
    console.log(options);
    throw new Error('Indisponível na web');
  }
}
