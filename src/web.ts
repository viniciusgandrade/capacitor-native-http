import { WebPlugin } from '@capacitor/core';

import type { HttpNativePlugin } from './definitions';

export class HttpNativeWeb extends WebPlugin implements HttpNativePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
