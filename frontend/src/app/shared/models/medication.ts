export type MedicationForm =
  | 'TABLET'
  | 'CAPSULE'
  | 'SYRUP'
  | 'SUSPENSION'
  | 'DROPS'
  | 'CREAM'
  | 'OINTMENT'
  | 'GEL'
  | 'SPRAY'
  | 'INHALER'
  | 'INJECTION'
  | 'POWDER'
  | 'SUPPOSITORY'
  | 'OTHER';

export type MedicationUnit = 'UNIT' | 'ML' | 'G';

export interface Medication {
  id: number;
  name: string;
  activeIngredient: string | null;
  strength: string | null;
  form: MedicationForm;
  unit: MedicationUnit;
  shelfLifeAfterOpeningDays: number | null;
  minimumQuantity: number | null;
}

export type MedicationRequest = Omit<Medication, 'id'>;
