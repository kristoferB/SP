describe('models page', function() {
    var tableRowsElem, noOfEntriesElem, initialArrayLength, initialEntriesCount;

    beforeAll(function() {
        browser.get('/models');
        tableRowsElem = element.all(by.repeater('m in vm.displayedModels'));
        noOfEntriesElem = element(by.binding('totalNoOfEntries()'));
        browser.waitForAngular();
        initialArrayLength = parseInt(tableRowsElem.count());
        initialEntriesCount = parseInt(noOfEntriesElem.getText());
    });

    it('should be the same number of entries as array length', function() {
        browser.waitForAngular();
        expect(initialArrayLength).toEqual(initialEntriesCount);
    });

    describe('create button click', function() {
        beforeAll(function() {
            element(by.id('open-create-model-dialog')).click();
        });

        describe('and form submit', function() {
            beforeAll(function() {
                browser.waitForAngular();
                element(by.model('vm.name')).sendKeys("My fancy model");
                element(by.id('create-model')).click();
            });

            it('should add one row to the models table', function() {
                browser.waitForAngular();
                this.newArrayLength = parseInt(tableRowsElem.count());
                expect(this.newArrayLength).toEqual(initialArrayLength + 1);
            });

            it('should increase the table entries count by 1', function() {
                browser.waitForAngular();
                this.newEntriesCount = parseInt(noOfEntriesElem.getText());
                expect(this.newEntriesCount).toEqual(initialEntriesCount + 1);
            });
        });
    });

    describe('delete button click', function() {
        var confirmDialog;

        beforeAll(function() {
            browser.waitForAngular();
            element.all(by.css('.btn-danger')).first().click();
            browser.sleep(500);
            confirmDialog = browser.switchTo().alert();
        });

        it('should open a confirm dialog with a warning message', function() {
            expect(confirmDialog.getText()).toContain('Are you sure');
        });

        describe('and OK button click', function() {
            beforeAll(function() {
                confirmDialog.accept();
            });

            it('should remove one row from the models table', function() {
                browser.waitForAngular();
                var newArrayLength = parseInt(tableRowsElem.count());
                expect(newArrayLength).toEqual(initialArrayLength - 1);
            });

            it('should lessen the table entries count by 1', function() {
                browser.waitForAngular();
                var newEntriesCount = parseInt(noOfEntriesElem.getText());
                expect(newEntriesCount).toEqual(initialEntriesCount - 1);
            });
        });
    });

    describe('open button click', function() {
        beforeAll(function() {
            browser.waitForAngular();
            element.all(by.css('.open-model')).first().click();
        });

        it('should change the active model from "None"', function() {
            browser.waitForAngular();
            expect(element(by.css('.fa-files-o')).getText()).not.toContain('None');
        });

        it('should change url to /dashboard', function() {
            browser.waitForAngular();
            expect(browser.getCurrentUrl()).toContain('/dashboard');
        });

    });
});
