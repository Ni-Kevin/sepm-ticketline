Cypress.Commands.add('loginAdmin', () => {
    cy.fixture('settings').then(settings => {
        cy.visit(settings.baseUrl);
        cy.contains('a', 'Login').click();
        cy.get('input[name="username"]').type(settings.adminUser);
        cy.get('input[name="password"]').type(settings.adminPw);
        cy.contains('button', 'Login').click();
        cy.url().should('include', '/#/news');
        cy.contains('a', 'My Profile').should('be.visible');
    })
})

Cypress.Commands.add('createNews', (msg) => {
    const title = 'title' + msg;
    const summary = 'summary' + msg;
    const text = 'text' + msg;

    cy.visit('/#/admin/news/new');
    cy.contains('New News Entry').should('be.visible');
    cy.get('input[name="title"]').type(title);
    cy.get('textarea[name="summary"]').type(summary);
    cy.get('textarea[name="text"]').type(text);
    cy.get('input#news-photo-input').selectFile({
        contents: Cypress.Buffer.from('news image ' + msg),
        fileName: 'news-image.png',
        mimeType: 'image/png',
        lastModified: Date.now()
    }, { force: true });
    cy.intercept('POST', '**/api/v1/news').as('createNews');
    cy.contains('button', 'Create news').click();

    cy.wait('@createNews').its('response.statusCode').should('eq', 201);
    cy.url().should('include', '/#/news');
    cy.contains(title).should('be.visible');
    cy.contains(summary).should('be.visible');
})
