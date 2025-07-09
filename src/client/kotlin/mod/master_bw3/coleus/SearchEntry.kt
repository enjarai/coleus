package mod.master_bw3.coleus

public data class SearchEntry (
    public val name: String,
    public val description: String,
    public val link: String,
    public val extraTerms: List<String> = listOf()
)