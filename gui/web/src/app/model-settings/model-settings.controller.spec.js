/* jshint -W117, -W030 */
describe('ModelSettingsController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.model-settings');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'ModelSettings'};
    });

    beforeEach(function () {
        controller = $controller('ModelSettingsController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('ModelSettings controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have title of ModelSettings', function() {
                expect(controller.title).to.equal('ModelSettings');
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });
        });
    });
});
