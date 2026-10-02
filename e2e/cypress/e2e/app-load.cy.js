context('app load', () => {
  it('loads the events page', () => {
    cy.visit('/#/events');

    cy.contains('a', 'Ticketline 4.0').should('be.visible');
    cy.contains('a', 'Events').should('be.visible');
  });
});
