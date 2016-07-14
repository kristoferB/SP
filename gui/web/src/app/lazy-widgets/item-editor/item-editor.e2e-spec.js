describe('item editor', function() {

    beforeAll(function() {
        browser.get('/dashboard');
    });

    describe('dropdown item click', function() {
        beforeAll(function() {
            element(by.id('new-widget-btn')).click();
            element(by.id('item-editor-dropdown-item')).click();
        });

        it('should open the widget', function() {
            expect(element(by.css('.item-editor')).isPresent()).toBeTruthy();
        });
    });

    describe('close button click', function() {
        beforeAll(function() {
            browser.actions().mouseMove(element(by.css('.panel-heading'))).perform();
            var button = element(by.css('.widget-close-btn'));
            browser.driver.wait(protractor.until.elementIsVisible(button), 3000);
            button.click();
        });

        it('should close the widget', function() {
            expect(element(by.css('.item-editor')).isPresent()).toBeFalsy();
        });
    })

});
