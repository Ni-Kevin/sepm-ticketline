declare namespace Cypress{
    interface Chainable {
        /**
         * Navigate to main page and login as admin
         */
        loginAdmin();

        /**
         * Creates a news entry with a given suffix
         * @param msg suffix used for the created news values
         */
        createNews(msg: string);
    }
}
