export interface CategorieSummary {
    id: number;
    designation: string;
}

export interface Article {
    id: number;
    codeArticle: string;
    designation : string;
    prixUnitaireHt: number;
    tauxTva: number;
    prixUnitaireTtc: number;
    photo : string | null;
    categorie: CategorieSummary;
}

export interface ArticleRequest {
    codeArticle: string;
    designation: string;
    prixUnitaireHt: number;
    tauxTva: number;
    photo: string | null;
    categorieId: number;
}