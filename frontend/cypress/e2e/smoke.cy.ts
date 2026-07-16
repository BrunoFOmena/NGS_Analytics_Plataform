describe('NGS Analytics smoke', () => {
  it('shows login page', () => {
    cy.visit('/login');
    cy.contains('NGS Analytics');
    cy.contains('Sign in');
  });
});
