export type MemberRole = 'OWNER' | 'MEMBER';

/** A household as seen by the current user (mirrors the API's `HouseholdResponse`). */
export interface Household {
  id: number;
  name: string;
  role: MemberRole;
}
