/* jshint -W117, -W030 */
describe('ItemEditorController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.itemEditor');
        bard.inject('$controller', '$log', '$rootScope', '$timeout');
        controller = $controller('ItemEditorController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('ItemEditorController', function() {

        describe('after activate', function() {
            it('should be created successfully', function () {
                expect(controller).to.be.defined;
            });
        });

        describe('editorLoaded', function() {
            beforeEach(function() {
                controller.editorLoaded({});
            });

            it('should load an editor instance', function() {
                expect(controller.editor).to.not.equal(null);
            });

            it('should log its addition', function() {
                expect($log.info.logs).to.match(/Added an Item Editor widget/);
            });
        });

        describe('setMode', function() {
            beforeEach(function() {
                controller.editor = {
                    editor: {
                        setOptions: sinon.spy(),
                        on: sinon.spy()
                    }
                };
            });

            describe('if called with argument \'text\'', function() {
                beforeEach(function() {
                    controller.setMode('text');
                });

                it('should change the editor mode option to \'text\'', function() {
                    expect(controller.options.mode).to.equal('text');
                });

                it('should not call ACE editor methods', function() {
                    expect(controller.editor.editor.setOptions.called).to.equal(false);
                    expect(controller.editor.editor.on.called).to.equal(false);
                });
            });

            describe('if called with argument \'code\'', function() {
                beforeEach(function() {
                    controller.setMode('code');
                    $timeout.flush();
                });

                it('should change the editor mode option to \'code\'', function() {
                    expect(controller.options.mode).to.equal('code');
                });

                it('should call ACE editor methods', function() {
                    expect(controller.editor.editor.setOptions.called).to.equal(true);
                    expect(controller.editor.editor.on.called).to.equal(true);
                });
            });

        });
    });
});
