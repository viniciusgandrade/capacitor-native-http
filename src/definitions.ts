export interface HttpNativePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
