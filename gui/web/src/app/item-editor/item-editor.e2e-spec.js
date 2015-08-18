describe('item editor widget', function() {

    beforeAll(function() {
        browser.get('/dashboard');
        element(by.id('new-widget-btn')).click();
        element(by.id('new-widget-dropdown')).all(by.css('li')).first().click();
    });

    it('should open an item editor widget', function() {
        expect(element(by.css('.item-editor')).isPresent()).toBeTruthy();
    });

});
