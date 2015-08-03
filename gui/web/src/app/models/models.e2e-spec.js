describe('models page', function() {
    browser.get('/models');

    describe('delete button click', function() {
        element(by.id('1-delete')).click();

        it('should open a confirm dialog with a warning message', function() {
            var alertDialog = browser.switchTo().alert();
            expect(alertDialog.getText()).toContain('Are you sure');
        });
    });
});
