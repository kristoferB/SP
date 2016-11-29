/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.kubInputGUII4')
      .controller('kubInputGUIControllerI4', kubInputGUIControllerI4);

    kubInputGUIControllerI4.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function kubInputGUIControllerI4($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what


        vm.a = 0;
        vm.b = 0;
        vm.result = [];
	vm.lock=1;
        vm.bigR = 255;
        vm.$var = 0;
        vm.buttonBG = "#ffffff"
        vm.resultColor = '#00FF00';
        vm.activeColour = 0;
	vm.backupTextInstruction="";
	vm.QueuePlacement='Last';	

	vm.Mode = 'Status';
	vm.Initialized = 0;
        //functions
	vm.setMode=setMode;
        vm.setActiveColor = setActiveColor;
        vm.saveNumber = saveNumber;
        vm.reset = reset;
        vm.tryTheTower = tryTheTower;
        vm.sendOrder = sendOrder;
        vm.UpdateCubes = UpdateCubes;
	vm.StopResume= StopResume;
	vm.Queue=Queue;
	//vm.NewOrder = NewOrder;

        //Contains the colours of the cubes
        vm.ButtonColour = {
Status: {
            Left: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Right: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Middle: [
                [0, 0, 0, 0]
            ]
        },
NewOrder: {
            Left: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Right: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Middle: [
                [0, 0, 0, 0]
            ]
        },
CurrentOrder: {
            Left: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Right: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ],
	    Middle: [
                [0, 0, 0, 0]
            ]
        }};

	vm.ColorControlValue= {Status: [0,0,0,0,0],
				NewOrder: [0,0,0,0,0],
				CurrentOrder: [0,0,0,0,0]
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
	    if(vm.Initialized ==0 && vm.Mode=='Status' || vm.Mode=='NewOrder' && vm.Initialized ==1){
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

        }
function setMode(Mode) {
	
	if (Mode == 'Status'){
	    vm.Mode = Mode;
	    document.getElementById('NewOrder').value='NewOrder';
	    document.getElementById('OrderButtonText').innerHTML='New Order';
	    document.getElementById('ResetButtonText').innerHTML='Reset all'; 
	    if (vm.Initialized ==0){
            	vm.Initialized = 1;
		vm.lock=1;
	    	document.getElementById('StatusButtonText').innerHTML='Status';
	    }
	    
	    document.getElementById('TextInstruction').innerHTML='Current status of the blocks';
	    document.getElementById('NewOrder').style.backgroundColor= "#ffffff";
	    document.getElementById('buttonCurrentOrder').style.backgroundColor= "#ffffff";
	    document.getElementById('Status').style.backgroundColor= "#5cd65c";
	    UpdateCubes(Mode);
	}
	else if (Mode == 'NewOrder' && vm.Initialized == 1){
		vm.Mode = Mode;
		document.getElementById('buttonCurrentOrder').style.backgroundColor= "#ffffff";
		
		if (document.getElementById('NewOrder').value=='NewOrder'){
			document.getElementById('ResetButtonText').innerHTML='Clear blocks'; 
			document.getElementById('TextInstruction').innerHTML='Reconfigure the blocks and place order';
			document.getElementById('Status').style.backgroundColor= "#ffffff";
			document.getElementById(Mode).style.backgroundColor= "#5cd65c";

			vm.ButtonColour[Mode]=$.extend(true,vm.ButtonColour['Status']);
			UpdateCubes(Mode);
			vm.ColorControlValue[Mode][1]=0;
			vm.ColorControlValue[Mode][2]=0;
			vm.ColorControlValue[Mode][3]=0;
			vm.ColorControlValue[Mode][4]=0;
		        document.getElementById(1).value=vm.ColorControlValue[Mode][1];
			document.getElementById(2).value=vm.ColorControlValue[Mode][2];
			document.getElementById(3).value=vm.ColorControlValue[Mode][3];
			document.getElementById(4).value=vm.ColorControlValue[Mode][4];
        		document.getElementById('NewOrder').value='PlaceOrder';
			document.getElementById('OrderButtonText').innerHTML='Place Order'; 
			vm.lock=0;
		}
		else if(document.getElementById('NewOrder').value=='PlaceOrder'){
			document.getElementById('NewOrder').style.backgroundColor= "#ffffff";
			document.getElementById('NewOrder').value='NewOrder';
			document.getElementById('OrderButtonText').innerHTML='New Order';
			UpdateCubes(Mode);
			document.getElementById('TextInstruction').innerHTML='Order placed';
			vm.lock=1;
		}

	}
	else if (Mode == 'CurrentOrder' && vm.Initialized == 1){
		vm.Mode = Mode;
		document.getElementById('OrderButtonText').innerHTML='New Order';
		document.getElementById('NewOrder').value='NewOrder';
		document.getElementById('TextInstruction').innerHTML='This is the current order under construction';
		document.getElementById('Status').style.backgroundColor= "#ffffff";
		document.getElementById('NewOrder').style.backgroundColor= "#ffffff";
		document.getElementById('buttonCurrentOrder').style.backgroundColor= "#5cd65c";
		document.getElementById('ResetButtonText').innerHTML='Terminate Order'; 
		UpdateCubes(Mode);
	}
	else if (Mode == 'StopResume' && vm.Initialized == 1){
	StopResume();
	}
}
        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }




    function onEvent() {
        //console.log("It has to be done");
        //reset();
    }

        function sendOrder() {

            var mess = {"data": {getNext: false, "buildOrder": vm.ButtonColour}};
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
        function setColor(btn,Mode,location, row, column, setOnly, reset) {
          
            var property = document.getElementById(btn);
	      if (reset) {
		if(Mode=='Status'){
			vm.lock=0;
            		vm.Initialized = 0;
	    		document.getElementById('StatusButtonText').innerHTML='Confirm';
	    		document.getElementById('TextInstruction').innerHTML='Initialize positions of the blocks and confirm';
			vm.ColorControlValue[Mode][1]=0;
			vm.ColorControlValue[Mode][2]=0;
			vm.ColorControlValue[Mode][3]=0;
			vm.ColorControlValue[Mode][4]=0;
			property.style.backgroundColor = "#FFFFFF";
                	vm.ButtonColour[Mode][location][row][column] = 0;
			document.getElementById('ProggressUpdate').innerHTML='No orders under construction';
		}
		else if(Mode=='NewOrder' && vm.lock==0){
			vm.ColorControlValue[Mode][1]=vm.ColorControlValue['Status'][1];
			vm.ColorControlValue[Mode][2]=vm.ColorControlValue['Status'][2];
			vm.ColorControlValue[Mode][3]=vm.ColorControlValue['Status'][3];
			vm.ColorControlValue[Mode][4]=vm.ColorControlValue['Status'][4];
			property.style.backgroundColor = "#FFFFFF";
                	vm.ButtonColour[Mode][location][row][column] = 0;
		}
            
            }
	    else{
	    	if(vm.Initialized ==0 && Mode=='Status' || Mode=='NewOrder' && vm.Initialized ==1 && vm.lock==0){
		    if (!setOnly && vm.activeColour>0) {
		        if(vm.ButtonColour[Mode][location][row][column] == vm.activeColour){
		            vm.ButtonColour[Mode][location][row][column] = 0;
			    if(Mode=='Status'){
			    vm.ColorControlValue[Mode][vm.activeColour]= parseInt(vm.ColorControlValue[Mode][vm.activeColour]) - 1;
			    }
			    else if(Mode=='NewOrder'){
				vm.ColorControlValue[Mode][vm.activeColour]= parseInt(vm.ColorControlValue[Mode][vm.activeColour]) + 1;
			    }
			}
		        else{
				if(vm.ButtonColour[Mode][location][row][column] != 0){
				   if(Mode=='Status'){
					vm.ColorControlValue[Mode][vm.ButtonColour[Mode][location][row][column]]=parseInt(vm.ColorControlValue[Mode][vm.ButtonColour[Mode][location][row][column]]) - 1;
					vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
				   }
				   else if(Mode=='NewOrder' && vm.ColorControlValue[Mode][vm.activeColour]>0){
					vm.ColorControlValue[Mode][vm.ButtonColour[Mode][location][row][column]]=parseInt(vm.ColorControlValue[Mode][vm.ButtonColour[Mode][location][row][column]]) + 1;
					vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
				   }
				}
                	
				if(Mode=='Status'){
					vm.ColorControlValue[Mode][vm.activeColour]=parseInt(vm.ColorControlValue[Mode][vm.activeColour]) + 1;
					vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
				   }
				   else if(Mode=='NewOrder' && vm.ColorControlValue[Mode][vm.activeColour]>0){
					vm.ColorControlValue[Mode][vm.activeColour]=parseInt(vm.ColorControlValue[Mode][vm.activeColour]) - 1;
					vm.ButtonColour[Mode][location][row][column] = vm.activeColour;
				   }
		     	}
		     }
		}
            switch (vm.ButtonColour[Mode][location][row][column]) {
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
	  }
	document.getElementById(1).value=vm.ColorControlValue[Mode][1];
	document.getElementById(2).value=vm.ColorControlValue[Mode][2];
	document.getElementById(3).value=vm.ColorControlValue[Mode][3];
	document.getElementById(4).value=vm.ColorControlValue[Mode][4];
	
        }

        function saveNumber(btn,location,row, column) {
	    setColor(btn,vm.Mode,location, row, column, 0, 0);
           //
            //tryTheTower(0);
        }

        /**
         * Updates the colour of the button,
         * without changing it!
         * @param row
         * @param column
         * @constructor
         */
        function UpdateCubes(Mode) {
            //setColor('button' + eval(row * 10 + column + 11),location, row, column, 1, 0);
            //tryTheTower(0);
	    for (var i = 0; i < 4; i++) {
		    setColor('button' + eval(i + 311),Mode,'Middle', 0, i, 1, 0);
                for (var j = 0; j < 4; j++) {
                    setColor('button' + eval(i * 10 + j + 111),Mode,'Left', i, j, 1, 0);
		    setColor('button' + eval(i * 10 + j + 211),Mode,'Right', i, j, 1, 0);
                }
            }
        }

        /**
         * Resets the colour of every button.
         */
        function reset() {
            for (var i = 0; i < 4; i++) {
		    setColor('button' + eval(i + 311),vm.Mode,'Middle', 0, i, 0, 1);
                for (var j = 0; j < 4; j++) {
                    setColor('button' + eval(i * 10 + j + 111),vm.Mode,'Left', i, j, 0, 1);
		    setColor('button' + eval(i * 10 + j + 211),vm.Mode,'Right', i, j, 0, 1);
                }
            }
            var property = document.getElementById('buttonBuild');
            property.style.backgroundColor = "#ffffff";
        }
	
	function StopResume(){
	if (document.getElementById('StopResumeButtonText').innerHTML=='Stop'){
		document.getElementById('StopResumeButtonText').innerHTML='Resume';
		document.getElementById('ProggressUpdate').innerHTML='All orders have been put on hold';
	}
	else{
	    document.getElementById('StopResumeButtonText').innerHTML='Stop';
	    document.getElementById('ProggressUpdate').innerHTML='Resuming operations';
	}
	}	

	function Queue(){
		switch(document.getElementById('buttonQueue').value){
			case "1":
				alert('hej');
				document.getElementById('buttonQueue').value='2';
				document.getElementById('buttonQueueText').innerHTML='Place first in Queue';
				break;
			case "2":
				document.getElementById('buttonQueue').value='3';
				document.getElementById('buttonQueueText').innerHTML='Place next in Queue';
				break;
			case "3": 
				document.getElementById('buttonQueue').value='1';
				document.getElementById('buttonQueueText').innerHTML='Place Last in Queue';
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
                for (var row = 1; row > -1; row--) {
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
                if (shallNotPass) {
                    alert('Your Tower Shall Not Pass!');
                } else if(anyCubesAtAll) {
                    sendOrder();
                }

                else
                    alert('You Need to Choose at Least One');
            }
        }


    }
})();
