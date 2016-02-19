/**
 * Created by hisp on 14/1/16.
 */

function contextMenoo(data) {
    this.menooData = data;
}
contextMenoo.prototype.init = function(){
    var elm = document.getElementById("contextMenoo");
    if (!elm){
        var div = document.createElement('div');
        div.id = "contextMenoo";
        document.body.appendChild(div);
    }
}

contextMenoo.prototype.draw = function(event){
    this.init();
    disable_scroll();
    var elm = document.getElementById("contextMenoo");
    elm.style.left= (event.pageX-0) + "px";
    elm.style.top = (event.pageY-0) + "px";

    var htmlMenoo = "<ul>";
    for (var i=0;i<this.menooData.length;i++){
        htmlMenoo = htmlMenoo+"<li id='"+this.menooData[i].id+"'>" ;

        //add icon
        if (this.menooData[i].icon || this.menooData[i].icon.length>0){

            switch(this.menooData[i].icon){
                case "trash" :  htmlMenoo = htmlMenoo+"<span><i class='fa fa-trash-o'></i></span>"
                    break;
                default : htmlMenoo = htmlMenoo+"<span><img height='5%' width='5%' src='"+this.menooData[i].icon+"'></span>"
            }
        }else{ // Add default Icon
            htmlMenoo = htmlMenoo+"<span><svg height='2' width='2'><line x1='0' x2='15' y1='0' y2='0' stroke-width='4' stroke='black' />" +
                "<line x1='0' x2='10' y1='0' y2='10' stroke-width='2' stroke='black' />"+
                "<line x1='10' x2='15' y1='10' y2='0' stroke-width='2' stroke='black' /></svg></span>"
        }
        htmlMenoo = htmlMenoo+" "+this.menooData[i].text+" </li>";
    }
    htmlMenoo = htmlMenoo+"</ul>";
    // $('#contextMenoo').innerHTML = htmlMenoo;
    document.getElementById('contextMenoo').innerHTML = htmlMenoo;
}

contextMenoo.prototype.finish = function(){
    enable_scroll();
$('#contextMenoo').remove();
}

$(document).mouseup(function (e)
{
    var container = $("#contextMenoo");

    if (!container.is(e.target) // if the target of the click isn't the container...
        && container.has(e.target).length === 0) // ... nor a descendant of the container
    {
        enable_scroll();
        container.remove();
    }
})

/* code for enabling disabling scrollbar - http://stackoverflow.com/users/378024/galambalazs */

// left: 37, up: 38, right: 39, down: 40,
// spacebar: 32, pageup: 33, pagedown: 34, end: 35, home: 36
var keys = [37, 38, 39, 40,32,33,34,35,36];

function preventDefault(e) {
    e = e || window.event;
    if (e.preventDefault)
        e.preventDefault();
    e.returnValue = false;
}

function keydown(e) {
    for (var i = keys.length; i--;) {
        if (e.keyCode === keys[i]) {
            preventDefault(e);
            return;
        }
    }
}

function wheel(e) {
    preventDefault(e);
}

function disable_scroll() {
    if (window.addEventListener) {
        window.addEventListener('DOMMouseScroll', wheel, false);
    }
    window.onmousewheel = document.onmousewheel = wheel;
    document.onkeydown = keydown;
}

function enable_scroll() {
    if (window.removeEventListener) {
        window.removeEventListener('DOMMouseScroll', wheel, false);
    }
    window.onmousewheel = document.onmousewheel = document.onkeydown = null;
}
/* END code for enabling disabling scrollbar - http://stackoverflow.com/users/378024/galambalazs */
