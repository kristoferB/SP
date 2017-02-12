/* jshint -W117, -W030 */
describe('ItemExplorerController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.itemExplorer');
        bard.inject('$controller', '$log', '$rootScope');
        controller = $controller('ItemExplorerController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('ItemExplorerController', function() {

        describe('after activate', function() {
            it('should be created successfully', function () {
                expect(controller).to.be.defined;
            });
        });

        describe('editorLoaded', function() {
            it('should log its addition', function() {
                expect($log.info.logs).to.match(/Added an Item Explorer widget/);
            });
        });
    });
});
