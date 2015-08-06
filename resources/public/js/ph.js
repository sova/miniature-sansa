/**move around the PH grid*/

var left_right_steps = 0;
var    up_down_steps = 0;

var    blurb_width = 306;
var    blurb_height = 190;

function leftArrowPressed() {
    // Upon a left Arrow (37) keypress, this function is invoked.
    console.log('moving left');
    left_right_steps += 1;
    $('#blurbpool').css('left', left_right_steps*blurb_width);
    
}

function rightArrowPressed() {
    console.log('right arrow pressed');
    left_right_steps -= 1;
    $('#blurbpool').css('left', left_right_steps * blurb_width);
}


function upArrowPressed() {
    console.log('up arrow pressed');
    up_down_steps += 1;
    $('#blurbpool').css('top', up_down_steps * blurb_height);
    
}


function downArrowPressed() {
    console.log('down arrow pressed');
    up_down_steps -= 1;
    $('#blurbpool').css('top', up_down_steps * blurb_height);
    
}

var topBoundary = 0;
var bottomBoundary = 4;
var leftBoundary = 0;
var rightBoundary = 4;

var blurbBuffer = 1;

var posLateral = 0, posVertical = 0;

function onMove( direction ) {
    posLateral =  (-1) * left_right_steps;
    posVertical = (-1) * up_down_steps;
    $('#status').text('position: '+ posLateral +' '+ posVertical);
    switch (direction) {
        case 'left':
          if (posLateral - blurbBuffer < leftBoundary) {
              //console.log("drawing blurbs pre-emptively to the left!");
              //draw blurbs to the left of leftBoundary
              leftBoundary -= 1;
              console.log("this is where you'd update the boundary ");              
          }
        case 'right':
          if (posLateral + blurbBuffer > rightBoundary) {
              //console.log("pre-emptively drawing blurbs to the right");
              //draw to the right
              rightBoundary += 1;
              }
        break;
        case 'up':
          if (posVertical - blurbBuffer < topBoundary) {
              //console.log("pre-emptively gon' draw more blurbs to the top");
              //draw up and above
              topBoundary -= 1;
          }
        break;
        case 'down':
          if (posVertical + blurbBuffer > bottomBoundary) {
              //console.log("gon' draw to da bottom yo!");
              //draw blurbs below boundary,
              bottomBoundary += 1;
              }
        break;
    }

}



function drawNewBlurbBox( ) {

    jQuery('<div/>', {
        id: 'blurb',
        href: 'http://google.com',
        title: 'Become a Googler',
        rel: 'external',
        text: 'Go to Google!'
    }).appendTo('#mySelector');
}

document.onkeypress = function(evt) {
    evt = evt || window.event;
    switch (evt.keyCode) {
    case 37:
        if ( posLateral > 0) {
            leftArrowPressed();
            onMove('left');
        }
        break;
    case 38:
        if (posVertical > 0) {
            upArrowPressed();
            onMove('up');
        } else { 
            //$('').css('border-top', '1px solid orange');//light up the boundary there and flash slow
            }
        break;
    case 39:
        if (posLateral < 2) {
            rightArrowPressed();
            onMove('right');
        }
        break;
    case 40:
        if (posVertical < 2) {
            downArrowPressed();
            onMove('down');
        }
        break;
    }
};

