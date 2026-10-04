import { TestBed } from '@angular/core/testing';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

export function registerFakeIcons(...names: string[]): void {
  const registry = TestBed.inject(MatIconRegistry);
  const sanitizer = TestBed.inject(DomSanitizer);
  for (const name of names) {
    registry.addSvgIconLiteral(name, sanitizer.bypassSecurityTrustHtml('<svg></svg>'));
  }
}
