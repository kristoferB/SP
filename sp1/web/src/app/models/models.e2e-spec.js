describe('models page', function() {
    var tableRowsElem, noOfEntriesElem, initialArrayLength, initialEntriesCount;

    beforeAll(function() {
        browser.get('/models');
        tableRowsElem = element.all(by.repeater('m in vm.displayedModels'));
        noOfEntriesElem = element(by.binding('totalNoOfEntries()'));
        initialArrayLength = parseInt(tableRowsElem.count());
        initialEntriesCount = parseInt(noOfEntriesElem.getText());
    });

    it('should be the same number of entries as array length', function() {
        expect(initialArrayLength).toEqual(initialEntriesCount);
    });

    describe('create button click', function() {
        beforeAll(function() {
            element(by.id('open-create-model-dialog')).click();
        });

        describe('and form submit', function() {
            beforeAll(function() {
                element(by.model('vm.name')).sendKeys('My fancy model');
                element(by.id('create-model')).click();
            });

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

    describe('model name click', function() {
        var nameLink, nameInput;

        beforeAll(function() {
            nameLink = element(by.css('.latest')).element(by.binding('m.name'));
            nameLink.click();
        });

        it('should make an empty text input show up', function() {
            nameInput = element(by.css('.latest')).element(by.css('.editable-input'));
            expect(nameInput.isPresent()).toBeTruthy();
        });

        describe(', some text input and focus lost', function() {
            beforeAll(function() {
                nameInput.clear();
                nameInput.sendKeys('A fancier name');
                element(by.css('.latest')).element(by.binding('m.attributes.time')).click();
            });

            it('should update the model name to "A fancier name"', function() {
                expect(nameLink.getText()).toContain('A fancier name');
            });
        });

    });

    describe('delete button click', function() {
        var confirmDialog;

        beforeAll(function() {
            element(by.css('.latest')).element(by.css('.btn-danger')).click();
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
        beforeAll(function() {
            element.all(by.css('.open-model')).first().click();
        });

        it('should change the active model from "None"', function() {
            expect(element(by.css('.fa-files-o')).getText()).not.toContain('None');
        });

        it('should change url to /dashboard', function() {
            expect(browser.getCurrentUrl()).toContain('/dashboard');
        });
    });
});
