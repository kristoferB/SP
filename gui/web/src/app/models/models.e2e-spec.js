describe('models page', function() {
    browser.get('/models');
    browser.sleep(2000);
    var tableRowsElem = element.all(by.repeater('m in vm.displayedModels'));
    var noOfEntriesElem = element(by.binding('totalNoOfEntries()'));
    var initialArrayLength = parseInt(tableRowsElem.count());
    var initialEntriesCount = parseInt(noOfEntriesElem.getText());

    it('should be the same number of entries as array length', function() {
        expect(initialArrayLength).toEqual(initialEntriesCount);
    });

    describe('create button click', function() {
        element(by.id('open-create-model-dialog')).click();
        browser.sleep(2000);

        describe('and form submit', function() {
            element(by.model('vm.name')).sendKeys("My fancy model");
            browser.sleep(2000);
            element(by.id('create-model')).click();
            browser.sleep(2000);

            it('should add one row to the models table', function() {
                this.newArrayLength = parseInt(tableRowsElem.count());
                expect(this.newArrayLength).toEqual(initialArrayLength + 1);
            });

            it('should increase the table entries count by 1', function() {
                this.newEntriesCount = parseInt(noOfEntriesElem.getText());
                expect(this.newEntriesCount).toEqual(initialEntriesCount + 1);
            });
        });
    });

    describe('delete button click', function() {
        element(by.css('.btn-danger')).click();
        browser.sleep(2000);
        var confirmDialog = browser.switchTo().alert();

        it('should open a confirm dialog with a warning message', function() {
            expect(confirmDialog.getText()).toContain('Are you sure');
        });

        describe('and OK button click', function() {
            confirmDialog.accept();
            browser.sleep(2000);

            it('should remove one row from the models table', function() {
                var newArrayLength = parseInt(tableRowsElem.count());
                expect(newArrayLength).toEqual(initialArrayLength - 1);
            });

            it('should lessen the table entries count by 1', function() {
                var newEntriesCount = parseInt(noOfEntriesElem.getText());
                expect(newEntriesCount).toEqual(initialEntriesCount - 1);
            });
        });
    });

    describe('open button click', function() {
        element(by.css('open-model')).click();
        browser.sleep(2000);

        it('should change the active model from "None"', function() {
            expect(element(by.css('fa-files-o')).getText()).not.toContain('None');
        });

        it('should change url to /dashboard', function() {
            expect(currentUrl()).toContain('/dashboard');
        });

    });
});
