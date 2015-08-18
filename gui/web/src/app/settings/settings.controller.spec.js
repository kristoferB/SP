/* jshint -W117, -W030 */
describe('SettingsController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.settings');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'Settings'};
    });

    beforeEach(function () {
        controller = $controller('SettingsController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('Settings controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have title of Settings', function() {
                expect(controller.title).to.equal('Settings');
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });
        });
    });
});
