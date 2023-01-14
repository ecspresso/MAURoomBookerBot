function check(checkbox) {
    let id = checkbox.id;
    let checked = checkbox.checked;
    let latest = localStorage.getItem("latest");
    let prev = localStorage.getItem("prev");

    if(checked) {
        if(prev != null) {
            oldbox = document.getElementById(prev);
            oldbox.checked = false;
        }

        if(latest != null) {
            localStorage.setItem("prev", latest);
        }

        localStorage.setItem("latest", id);
    } else { // checked
        if(id === prev) {
            localStorage.removeItem("prev");
        } else { // id === latest
            if(prev != null) {
                localStorage.setItem("latest", prev);
                localStorage.removeItem("prev");
            } else { // prev === null
                localStorage.removeItem("latest");
            }
        }
    }
}

let t = document.getElementsByTagName("input");
for(let i = 1; i < t.length; i++) {
    t[i].checked = false;
}


let p = localStorage.getItem("prev");
let l = localStorage.getItem("latest");

if(p != null) {
    let cbp = document.getElementById(p);
    cbp.checked = true;
    }

if(l != null) {
    let cbp = document.getElementById(l);
    cbp.checked = true;
}