import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

const ICONS = ['add', 'arrow_back', 'delete', 'edit', 'google', 'logout'];

export function provideIcons(): EnvironmentProviders {
  return provideAppInitializer(() => {
    const registry = inject(MatIconRegistry);
    const sanitizer = inject(DomSanitizer);
    for (const name of ICONS) {
      registry.addSvgIcon(name, sanitizer.bypassSecurityTrustResourceUrl(`icons/${name}.svg`));
    }
  });
}
