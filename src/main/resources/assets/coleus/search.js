import {
    create,
    search,
    insert,
} from "https://cdn.jsdelivr.net/npm/@orama/orama@3.1.10/+esm";

import { Highlight } from 'https://cdn.jsdelivr.net/npm/@orama/highlight@0.1.9/+esm'

const db = create({
    schema: {
        name: "string",
        description: "string",
        extraTerms: "string[]",
    },
});


window.addEventListener("load", () => {
    let assetsUrl = document.getElementById("search-script").dataset.assetspath


    fetch(assetsUrl + '/searchEntries.json')
        .then(response => {
            return response.json();
        }).then(data => {
            for (const entry of data) {
                insert(db, {
                    name: entry.name,
                    description: entry.description,
                    extraTerms: entry.extraTerms,
                });
            }
        })

    let searchButton = document.getElementById("search-button")
    searchButton.addEventListener("click", () => {
        if (document.getElementById("search-overlay") !== null) return

        let searchOverlay = document.createElement("div")
        searchOverlay.id = "search-overlay"
        document.getElementsByClassName("page")[0].appendChild(searchOverlay)

        let searchBox = document.createElement("input")
        searchBox.id = "search"
        searchOverlay.appendChild(searchBox)
        searchBox.focus()


        let resultContainer = document.createElement("div")
        resultContainer.id = "search-results"
        searchOverlay.appendChild(resultContainer)

        searchBox.addEventListener("input", () => {
            const searchResults = search(db, {
                term: searchBox.value,
            });

            resultContainer.innerHTML = ''
            //console.log(searchResults)

            for (const entry of searchResults.hits) {
                const highlighter = new Highlight()
                const highlighted = highlighter.highlight(
                    entry.document.name + "\n" + entry.document.description, 
                    searchBox.value
                )
                console.log(highlighted.positions)
                console.log(highlighted.trim(100))

                let entryDiv = document.createElement("div") 

                let name = document.createElement("h4")
                name.textContent = entry.document.name
                entryDiv.appendChild(name)

                let description = document.createElement("p")
                description.innerHTML = highlighted.trim(100)
                entryDiv.appendChild(description)

                resultContainer.appendChild(entryDiv)
            }
        })
    })
})