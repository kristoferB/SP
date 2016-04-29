/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.kubInputGUI')
      .controller('kubInputGUIController', kubInputGUIController);

    kubInputGUIController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function kubInputGUIController($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what


        vm.a = 0;
        vm.b = 0;
        vm.result = [];

        vm.bigR = 255;
        vm.$var = 0;
        vm.buttonBG = "#ffffff"
        vm.resultColor = '#00FF00';
        vm.activeColour = 0;

        //functions
        vm.setActiveColor = setActiveColor;
        vm.saveNumber = saveNumber;
        vm.reset = reset;
        vm.tryTheTower = tryTheTower;
        vm.randomTower = randomTower;
        vm.sendOrder = sendOrder;
        vm.UpdateCubes = UpdateCubes;
        vm.preDefined = preDefined;

        //Contains the colours of the cubes
        vm.ButtonColour = {
            kub: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ]
        };
        /*
        En sak som bör fixas är att färgerna iallafall har rätt nummer så de är i bokstavsordning
        1 = Blue, 2 = Green, 3 = Red, 4 = Yellow
        0 = Tom/vit
         */

        //Remove these when done
        vm.debug = 0;
        vm.debug2 = 0;

        activate();

        function setActiveColor(int) {
            vm.activeColour = int;
            switch (vm.activeColour) {
                case 1://YELLOW = 1
                    vm.buttonBG = "#ffff66";
                    break;
                case 2://GREEN = 2
                    vm.buttonBG = "#5cd65c";
                    break;
                case 3://RED = 3
                    vm.buttonBG = "#ff3333";
                    break;
                case 4://BLUE = 4
                    vm.buttonBG = "#0066ff";
                    break;
                default:
                    vm.buttonBG= "#FFFFFF";
            }

        }

        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });

        }

        function sendOrder() {

            var mess = {"data": {getNext: false, "buildOrder": vm.ButtonColour.kub}};
            spServicesService.callService(spServicesService.getService("operatorService"),
                mess,
                function (resp) {
                    if (_.has(resp, 'attributes.result')) {
                        console.log("Hej" + vm.result);
                    }
                }
            )
        }

        /**
         * Change the colour of a button.
         * @param btn
         * @param row
         * @param column
         * @param reset
         */
        function setColor(btn, row, column, setOnly, reset) {
            var property = document.getElementById(btn);
            if (!setOnly) {
                //vm.ButtonColour.kub[row][column] = ++vm.ButtonColour.kub[row][column] % 5;
                if(vm.ButtonColour.kub[row][column] == vm.activeColour)
                    vm.ButtonColour.kub[row][column] = 0;
                else
                vm.ButtonColour.kub[row][column] = vm.activeColour;
            }
            switch (vm.ButtonColour.kub[row][column]) {
                case 1://YELLOW = 1
                    property.style.backgroundColor = "#ffff66";
                    break;
                case 2://GREEN = 2
                    property.style.backgroundColor = "#5cd65c";
                    break;
                case 3://RED = 3
                    property.style.backgroundColor = "#ff3333";
                    break;
                case 4://BLUE = 4
                    property.style.backgroundColor = "#0066ff";
                    break;
                default: //white by default
                    property.style.backgroundColor = "#FFFFFF";
            }
            if (reset) {
                property.style.backgroundColor = "#FFFFFF"
                vm.ButtonColour.kub[row][column] = 0;
            }
        }

        /**
         * Updates the colour of the button,
         * It will change the colour!
         * @param row
         * @param column
         */
        function saveNumber(row, column) {
            setColor('button' + eval(row * 10 + column + 11), row, column, 0, 0);
            tryTheTower(0);
        }

        /**
         * Updates the colour of the button,
         * without changing it!
         * @param row
         * @param column
         * @constructor
         */
        function UpdateCubes(row, column) {
            setColor('button' + eval(row * 10 + column + 11), row, column, 1, 0);
            tryTheTower(0);
        }

        /**
         * Resets the colour of every button.
         */
        function reset() {
            for (var i = 0; i < 4; i++) {
                for (var j = 0; j < 4; j++) {
                    setColor('button' + eval(i * 10 + j + 11), i, j, 0, 1);
                }
            }
            var property = document.getElementById('buttonBuild');
            property.style.backgroundColor = "#ffffff";
        }

        /**
         * Creates a predefined tower.
         */
        function preDefined(number) {
            switch(number) {
                case 1://The Swedish flag.
                    reset();
                    for (var column = 0; column < 4; column++) {
                        for (var row = 2; row > -1; row--) {
                            if(row == 1 || column == 1) vm.ButtonColour.kub[row][column] = 1;
                            else vm.ButtonColour.kub[row][column] = 4;
                            UpdateCubes(row, column);
                        }
                    }
                    break;
                case 2://Plains, sky and sun.
                    reset();
                    for (var column = 0; column < 4; column++) {
                        for (var row = 3; row > -1; row--) {
                            if(row == 0) vm.ButtonColour.kub[row][column] = 2;
                            else if(row > 1 && column > 1)vm.ButtonColour.kub[row][column] = 1;
                            else vm.ButtonColour.kub[row][column] = 4;
                            UpdateCubes(row, column);
                        }
                    }
                    break;
            }

        }

        /**
         * Checks if the tower is allowed to be built and
         * updates the build-button colour accordingly.
         * It also displays some messages.
         * @param display
         */
        function tryTheTower(display) {
            var property = document.getElementById('buttonBuild');
            var shallNotPass = 0;
            var anyCubesAtAll = 0;
            for (var column = 0; column < 4; column++) {
                var temp = 0;
                for (var row = 3; row > -1; row--) {
                    if (temp && !vm.ButtonColour.kub[row][column]) {
                        shallNotPass = 1;
                    }
                    if (vm.ButtonColour.kub[row][column]) {
                        temp = 1;
                        anyCubesAtAll = 1;
                    }
                }
            }
            if(shallNotPass)
                property.style.backgroundColor = "#ff3333";
            else if(anyCubesAtAll)
                property.style.backgroundColor = "#66ff66";
            else
                property.style.backgroundColor = "#ffffff";
            if(display) {
                if (shallNotPass)
                    alert('Your Tower Shall Not Pass!');
                else if(anyCubesAtAll) {
                    sendOrder();
                    alert('OK');
                }

                else
                    alert('You Need to Choose at Least One');
            }
        }

        /**
         * Creates a random coloured tower.
         */
        function randomTower() {
            for (var column = 0; column < 4; column++) {
                var stopPlacingCubes = 0;
                for (var row = 0; row < 4; row++) {
                    var tempColour = Math.floor((Math.random() * 5));
                    vm.debug = tempColour;
                    if(tempColour && !stopPlacingCubes){
                        vm.ButtonColour.kub[row][column] = tempColour;
                        vm.debug = tempColour;
                    }
                    else{
                        vm.ButtonColour.kub[row][column] = 0;
                        stopPlacingCubes = 1;
                    }
                    UpdateCubes(row, column);
                }
            }
        }

    }
})();
